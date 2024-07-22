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
import com.amazonappstore.hellopizza.data.HelloPizzaStateModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HelloPizzaRepository(
    private val billingRemoteDataSource: BillingDataSource,
    private val helloPizzaStateModel: HelloPizzaStateModel,
    private val applicationScope: CoroutineScope
) {

    val billingLifecycleObserver: BillingDataSource get() = billingRemoteDataSource

    init {
        collectConsumedPurchases()
    }

    private fun isPurchased(sku: String): Flow<Boolean> =
        billingRemoteDataSource.isProductPurchased(sku)

    private fun collectConsumedPurchases() = applicationScope.launch {
        billingRemoteDataSource.consumedPurchasesFlow
            .onEach { consumedSkus ->
                consumedSkus.forEach { sku ->
                    if (sku == SKU_PIZZA) helloPizzaStateModel.incrementPizzas(MAX_PIZZAS)
                }
            }
            .collect {}
    }

    fun buySku(activity: Activity, sku: String) {
        applicationScope.launch {
            billingRemoteDataSource.startBillingFlow(activity, sku)
        }
    }

    fun isPizzaPurchased(): Flow<Boolean> {
        return isPurchased(SKU_PIZZA)
    }

    fun isPizzaDiscountPurchased(): Flow<Boolean> {
        return isPurchased(SKU_PIZZA_DISCOUNT)
    }

    fun isInfinitePizzaYearlyPurchased(): Flow<Boolean> {
        return isPurchased(SKU_INFINITE_PIZZA_YEARLY)
    }


    companion object {
        const val MAX_PIZZAS = 30

        const val SKU_PIZZA_DISCOUNT = "pizza_discount"
        const val SKU_PIZZA = "one_pizza"

        const val SKU_INFINITE_PIZZA_YEARLY = "pizza_yearly"
        val SKUS = arrayOf(SKU_PIZZA_DISCOUNT, SKU_PIZZA)

    }
}
