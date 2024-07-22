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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface BillingDataSource {
    val newPurchasesFlow: Flow<List<String>>
    val isBillingFlowInProcess: Flow<Boolean>
    val consumedPurchasesFlow: Flow<List<String>>

    suspend fun refreshPurchasesAsync()
    suspend fun startBillingFlow(activity: Activity?, sku: String)
    suspend fun consumeInAppPurchaseAsync(sku: String)

    fun isProductPurchased(sku: String): Flow<Boolean>
    fun isProductPurchasable(sku: String): Flow<Boolean>
    fun getProductTitle(sku: String): Flow<String>
    fun getProductPrice(sku: String): Flow<String>
    fun getProductDescription(sku: String): Flow<String>
}