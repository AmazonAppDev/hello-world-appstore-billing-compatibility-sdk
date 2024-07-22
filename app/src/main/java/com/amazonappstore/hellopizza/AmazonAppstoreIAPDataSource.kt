/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazonappstore.hellopizza

import android.app.Activity
import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.amazon.device.iap.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val RECONNECT_TIMER_START_MS = 1_000L
private const val RECONNECT_TIMER_MAX_MS = 1_000L * 60 * 15
private const val SKU_DETAILS_REPEAT_QUERY_MS = 1_000L * 60 * 60 * 4

class AmazonAppstoreIAPDataSource(
    application: Application,
    private val defaultScope: CoroutineScope,
    inAppSkus: Array<String>? = null,
    subscriptionSkus: String? = null
) : BillingDataSource, DefaultLifecycleObserver, PurchasesUpdatedListener,
    BillingClientStateListener {

    private val billingClient = BillingClient.newBuilder(application)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val knownInAppSKUs = inAppSkus?.toList() ?: emptyList()
    private val knownSubscriptionSKUs = subscriptionSkus ?: ""

    private var reconnectMilliseconds = RECONNECT_TIMER_START_MS
    private var skuDetailsResponseTime = -SKU_DETAILS_REPEAT_QUERY_MS

    private val skuStateMap = mutableMapOf<String, MutableStateFlow<SkuState>>()
    private val skuDetailsMap = mutableMapOf<String, MutableStateFlow<SkuDetails?>>()

    private val purchaseConsumptionInProcess = mutableSetOf<Purchase>()
    private val _newPurchaseFlow = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)
    private val _purchaseConsumedFlow = MutableSharedFlow<List<String>>()
    private val _billingFlowInProcess = MutableStateFlow(false)

    override val newPurchasesFlow: Flow<List<String>> = _newPurchaseFlow
    override val isBillingFlowInProcess: Flow<Boolean> = _billingFlowInProcess
    override val consumedPurchasesFlow: Flow<List<String>> = _purchaseConsumedFlow

    init {
        initializeFlows()
        billingClient.startConnection(this)
    }

    private enum class SkuState {
        UNPURCHASED, PENDING, PURCHASED, PURCHASED_AND_ACKNOWLEDGED
    }

    /**
     * Sets the SKU state based on the provided purchase.
     *
     * @param purchase The purchase object containing the SKUs and their states.
     */
    private fun setSkuStateFromPurchase(purchase: Purchase) {
        purchase.skus.forEach { sku ->
            skuStateMap[sku]?.tryEmit(
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PENDING -> SkuState.PENDING
                    Purchase.PurchaseState.UNSPECIFIED_STATE -> SkuState.UNPURCHASED
                    Purchase.PurchaseState.PURCHASED -> SkuState.PURCHASED
                    else -> SkuState.UNPURCHASED
                }
            )
        }
    }

    /**
     * Sets the SKU state for the specified SKU.
     *
     * @param sku The SKU to set the state for.
     * @param newSkuState The new state to set for the SKU.
     */
    private fun setSkuState(sku: String, newSkuState: SkuState) {
        skuStateMap[sku]?.tryEmit(newSkuState)
    }

    /**
     * Consumes the provided purchase.
     *
     * @param purchase The purchase to consume.
     */
    private suspend fun consumePurchase(purchase: Purchase) {
        if (purchaseConsumptionInProcess.contains(purchase)) return

        purchaseConsumptionInProcess.add(purchase)
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            purchaseConsumptionInProcess.remove(purchase)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                defaultScope.launch {
                    _purchaseConsumedFlow.emit(purchase.skus)
                }
                purchase.skus.forEach { sku ->
                    setSkuState(sku, SkuState.UNPURCHASED)
                }
            }
        }
    }

    /**
     * Processes the provided list of purchases and updates the SKU stat     *
     * @param purchases The list of purchases to process.
     * @param skusToUpdate The list of SKUs to update.
     */
    private fun processPurchaseList(purchases: List<Purchase>?, skusToUpdate: List<String>?) {
        val updatedSkus = mutableSetOf<String>()
        purchases?.forEach { purchase ->
            purchase.skus.forEach { sku ->
                if (skuStateMap.containsKey(sku)) {
                    updatedSkus.add(sku)
                }
            }
            setSkuStateFromPurchase(purchase)
        }
        skusToUpdate?.forEach { sku ->
            if (!updatedSkus.contains(sku)) {
                setSkuState(sku, SkuState.UNPURCHASED)
            }
        }
    }

    /**
     * Refreshes the purchases asynchronously.
     */
    override suspend fun refreshPurchasesAsync() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchaseList(purchases, knownInAppSKUs)
            }
        }
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchaseList(purchases, listOf(knownSubscriptionSKUs))
            }
        }
    }

    /**
     * Starts the billing flow for the specified SKU.
     *
     * @param activity The activity to launch the billing flow from.
     * @param sku The SKU to start the billing flow for.
     */
    override suspend fun startBillingFlow(activity: Activity?, sku: String) {
        skuDetailsMap[sku]?.value?.let { skuDetails ->
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
            val billingResult = billingClient.launchBillingFlow(activity!!, billingFlowParams)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _billingFlowInProcess.emit(true)
            }
        }
    }

    /**
     * Consumes the in-app purchase for the specified SKU asynchronously.
     *
     * @param sku The SKU of the in-app purchase to consume.
     */
    override suspend fun consumeInAppPurchaseAsync(sku: String) {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.filter { purchase -> purchase.skus.contains(sku) }
                    .forEach { purchase -> defaultScope.launch { consumePurchase(purchase) } }
            }
        }
    }

    /**
     * Checks if the specified product is purchased.
     *
     * @param sku The SKU of the product to check.
     * @return A flow emitting a boolean indicating if the product is purchased.
     */
    override fun isProductPurchased(sku: String): Flow<Boolean> {
        return skuStateMap[sku]?.map { it == SkuState.PURCHASED || it == SkuState.PURCHASED_AND_ACKNOWLEDGED }
            ?: flowOf(false)
    }

    /**
     * Checks if the specified product is purchasable.
     *
     * @param sku The SKU of the product to check.
     * @return A flow emitting a boolean indicating if the product is purchasable.
     */
    override fun isProductPurchasable(sku: String): Flow<Boolean> {
        return skuStateMap[sku]?.map { it == SkuState.UNPURCHASED } ?: flowOf(false)
    }

    /**
     * Retrieves the title of the specified product.
     *
     * @param sku The SKU of the product.
     * @return A flow emitting the title of the product.
     */
    override fun getProductTitle(sku: String): Flow<String> {
        return skuDetailsMap[sku]?.mapNotNull { it?.title } ?: flowOf("")
    }

    /**
     * Retrieves the price of the specified product.
     *
     * @param sku The SKU of the product.
     * @return A flow emitting the price of the product.
     */
    override fun getProductPrice(sku: String): Flow<String> {
        return skuDetailsMap[sku]?.mapNotNull { it?.price } ?: flowOf("")
    }

    /**
     * Retrieves the description of the specified product.
     *
     * @param sku The SKU of the product.
     * @return A flow emitting the description of the product.
     */
    override fun getProductDescription(sku: String): Flow<String> {
        return skuDetailsMap[sku]?.mapNotNull { it?.description } ?: flowOf("")
    }

    /**
     * Initializes the SKU state and details flows.
     */
    private fun initializeFlows() {
        knownInAppSKUs.forEach { sku ->
            skuStateMap[sku] = MutableStateFlow(SkuState.UNPURCHASED)
            skuDetailsMap[sku] = MutableStateFlow(null)
        }

        skuStateMap[knownSubscriptionSKUs] = MutableStateFlow(SkuState.UNPURCHASED)
        skuDetailsMap[knownSubscriptionSKUs] = MutableStateFlow(null)

        skuDetailsMap.values.forEach { detailsFlow ->
            detailsFlow.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .onEach { isActive ->
                    if (isActive && System.currentTimeMillis() - skuDetailsResponseTime > SKU_DETAILS_REPEAT_QUERY_MS) {
                        skuDetailsResponseTime = System.currentTimeMillis()
                        querySkuDetailsAsync()
                    }
                }
                .launchIn(defaultScope)
        }
    }

    /**
     * Queries the SKU details asynchronously.
     */
    private fun querySkuDetailsAsync() {
        if (knownInAppSKUs.isNotEmpty()) {
            val inAppSkuDetailsParams = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(knownInAppSKUs)
                .build()
            billingClient.querySkuDetailsAsync(inAppSkuDetailsParams) { result, details ->
                onSkuDetailsResponse(result, details)
            }
        }

        if (knownSubscriptionSKUs.isNotEmpty()) {
            val subscriptionSkuDetailsParams = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.SUBS)
                .setSkusList(listOf(knownSubscriptionSKUs))
                .build()
            billingClient.querySkuDetailsAsync(subscriptionSkuDetailsParams) { result, details ->
                onSkuDetailsResponse(result, details)
            }
        }
    }

    /**
     * Handles the SKU details response.
     *
     * @param billingResult The billing result.
     * @param skuDetailsList The list of SKU details.
     */
    private fun onSkuDetailsResponse(
        billingResult: BillingResult,
        skuDetailsList: List<SkuDetails>?
    ) {
        skuDetailsResponseTime = when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                skuDetailsList?.forEach { skuDetails ->
                    skuDetailsMap[skuDetails.sku]?.tryEmit(skuDetails)
                }
                System.currentTimeMillis()
            }

            else -> {
                -SKU_DETAILS_REPEAT_QUERY_MS
            }
        }
    }

    /**
     * Handles the purchases updated event.
     *
     * @param billingResult The billing result.
     * @param purchases The list of updated purchases.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.let { processPurchaseList(it, null) }
            else -> {}
        }
        defaultScope.launch {
            _billingFlowInProcess.emit(false)
        }
    }


    /**
     * Handles the billing service disconnected event.
     *
     * For the Appstore Billing Compatibility SDK, this is never actually invoked. See:
     * https://developer.amazon.com/docs/in-app-purchasing/implement-google-play-billing.html#connect-to-the-amazon-appstore
     */
    override fun onBillingServiceDisconnected() {
        // no-op
    }

    /**
     * Handles the billing setup finished event.
     *
     * @param billingResult The billing result.
     */
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                reconnectMilliseconds = RECONNECT_TIMER_START_MS
                defaultScope.launch {
                    querySkuDetailsAsync()
                    refreshPurchasesAsync()
                }
            }

            else -> {
                onBillingServiceDisconnected()
            }
        }
    }

    /**
     * Handles the resume event of the lifecycle.
     *
     * @param owner The lifecycle owner.
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (!_billingFlowInProcess.value) {
            if (billingClient.isReady) {
                defaultScope.launch {
                    refreshPurchasesAsync()
                }
            }
        }
    }

    companion object {
        /**
         * Creates an instance of the [AmazonAppstoreIAPDataSource].
         *
         * @param application The application instance.
         * @param defaultScope The default coroutine scope.
         * @param inAppSkus The list of in-app SKUs.
         * @param subscriptionSkus The list of subscription SKUs.
         * @return The created [AmazonAppstoreIAPDataSource] instance.
         */
        fun getInstance(
            application: Application,
            defaultScope: CoroutineScope,
            inAppSkus: Array<String>?,
            subscriptionSkus: String
        ): BillingDataSource {
            return AmazonAppstoreIAPDataSource(
                application,
                defaultScope,
                inAppSkus,
                subscriptionSkus
            )
        }
    }
}