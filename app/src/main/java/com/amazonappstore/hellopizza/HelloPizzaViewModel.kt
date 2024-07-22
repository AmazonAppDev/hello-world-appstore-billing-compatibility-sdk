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
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.Flow

class HelloPizzaViewModel(private val repository: HelloPizzaRepository) : ViewModel() {

    val billingLifecycleObserver: LifecycleObserver =
        repository.billingLifecycleObserver as LifecycleObserver


    val isPizzaPurchased: Flow<Boolean> = repository.isPizzaPurchased()
    val isPizzaDiscountPurchased: Flow<Boolean> = repository.isPizzaDiscountPurchased()
    val isInfinitePizzaYearlyPurchased: Flow<Boolean> = repository.isInfinitePizzaYearlyPurchased()


    fun buySku(activity: Activity, sku: String) = repository.buySku(activity, sku)

    class HelloPizzaViewModelFactory(private val repository: HelloPizzaRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HelloPizzaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HelloPizzaViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}