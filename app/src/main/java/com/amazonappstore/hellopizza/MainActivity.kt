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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amazonappstore.hellopizza.ui.theme.HelloPizzaTheme

class MainActivity : ComponentActivity() {

    private val helloPizzaViewModel: HelloPizzaViewModel by viewModels {
        HelloPizzaViewModel.HelloPizzaViewModelFactory(
            (application as HelloPizzaApplication).appContainer.helloPizzaRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(helloPizzaViewModel.billingLifecycleObserver)

        setContent {
            val isPizzaPurchased by helloPizzaViewModel.isPizzaPurchased.collectAsState(initial = false)
            val isPizzaDiscountPurchased by helloPizzaViewModel.isPizzaDiscountPurchased.collectAsState(
                initial = false
            )
            val isInfinitePizzaYearlyPurchased by helloPizzaViewModel.isInfinitePizzaYearlyPurchased.collectAsState(
                initial = false
            )

            HelloPizzaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PizzaScreen(
                        isPizzaPurchased,
                        isPizzaDiscountPurchased,
                        isInfinitePizzaYearlyPurchased,
                        {
                            helloPizzaViewModel.buySku(
                                this@MainActivity,
                                HelloPizzaRepository.SKU_PIZZA
                            )
                        },
                        {
                            helloPizzaViewModel.buySku(
                                this@MainActivity,
                                HelloPizzaRepository.SKU_PIZZA_DISCOUNT
                            )
                        },
                        {
                            helloPizzaViewModel.buySku(
                                this@MainActivity,
                                HelloPizzaRepository.SKU_INFINITE_PIZZA_YEARLY
                            )
                        })
                }
            }
        }
    }
}

@Composable
fun PizzaScreen(
    hasBoughtPizza: Boolean,
    hasDiscount: Boolean,
    hasSubscription: Boolean,
    onBuyPizza: () -> Unit,
    onBuyPizzaDiscount: () -> Unit,
    onBuySubscription: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            val imageSrc = if (hasSubscription) {
                R.drawable.pizza_subscription
            } else if (hasDiscount) {
                R.drawable.golden_pizza
            } else {
                R.drawable.pizza_slice
            }

            val whatYouPurchased = if (hasBoughtPizza) {
                if (hasDiscount) {
                    stringResource(R.string.subscription_confirmed)
                } else {
                    stringResource(R.string.pizza_confirmed)
                }
            } else {
                stringResource(R.string.nothing)
            }

            Box(
                modifier = Modifier
                    .size(200.dp)
            ) {
                Image(
                    painter = painterResource(id = imageSrc),
                    contentDescription = "Pizza",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You purchased: $whatYouPurchased",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onBuyPizza,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                ) {
                    Text(text = stringResource(R.string.buy_pizza))
                }
                Button(
                    onClick = onBuyPizzaDiscount,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                ) {
                    Text(text = stringResource(R.string.buy_discount))
                }
                Button(
                    onClick = onBuySubscription,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(text = stringResource(R.string.buy_subscription))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PizzaScreenPreview() {
    HelloPizzaTheme {
        PizzaScreen(true, false, false, {}, {}, {})
    }
}