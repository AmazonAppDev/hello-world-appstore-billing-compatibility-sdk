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

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import com.amazonappstore.hellopizza.data.HelloPizzaStateModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HelloPizzaApplication : Application() {
    lateinit var appContainer: AppContainer


    inner class AppContainer(private val billingDataSource: BillingDataSource) {
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private val helloPizzaStateModel = HelloPizzaStateModel(this@HelloPizzaApplication)
        val helloPizzaRepository = HelloPizzaRepository(
            billingDataSource,
            helloPizzaStateModel,
            applicationScope
        )
    }

    private fun getInstallerPackageName(): String? {
        return try {
            val pm = packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun onCreate() {
        super.onCreate()

        val billingDataSource = AmazonAppstoreIAPDataSource.getInstance(
            this@HelloPizzaApplication,
            CoroutineScope(SupervisorJob() + Dispatchers.IO),
            HelloPizzaRepository.SKUS,
            HelloPizzaRepository.SKU_INFINITE_PIZZA_YEARLY
        )

        appContainer = AppContainer(billingDataSource)
    }
}