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

package com.amazonappstore.hellopizza.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HelloPizzaDao {
    @Query("SELECT `value` FROM HelloPizzaState WHERE `key` = :key LIMIT 1")
    operator fun get(key: String): Flow<Int>

    @Query("REPLACE INTO HelloPizzaState VALUES(:key,:value)")
    fun put(key: String, value: Int)

    @Query("UPDATE HelloPizzaState SET `value`=`value`-1 WHERE `key`=:key AND `value` > :minValue")
    fun decrement(key: String, minValue: Int): Int

    @Query("UPDATE HelloPizzaState SET `value`=`value`+1 WHERE `key`=:key AND `value` < :maxValue")
    fun increment(key: String, maxValue: Int): Int

}
