/**
 * Copyright 2016 Bartosz Schiller
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pdfnotemate.tools.pdf.viewer.util

object MathUtils {
    private const val BIG_ENOUGH_INT = 16 * 1024
    private const val BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT.toDouble()
    private const val BIG_ENOUGH_CEIL = 16384.999999999996

    /**
     * Limits the given **number** between the other values
     * @param number  The number to limit.
     * @param between The smallest value the number can take.
     * @param and     The biggest value the number can take.
     * @return The limited number.
     */
    @JvmStatic
    fun limit(number: Int, between: Int, and: Int): Int {
        if (number <= between) {
            return between
        }
        return if (number >= and) {
            and
        } else {
            number
        }
    }

    /**
     * Limits the given **number** between the other values
     * @param number  The number to limit.
     * @param between The smallest value the number can take.
     * @param and     The biggest value the number can take.
     * @return The limited number.
     */
    @JvmStatic
    fun limit(number: Float, between: Float, and: Float): Float {
        if (number <= between) {
            return between
        }
        return if (number >= and) {
            and
        } else {
            number
        }
    }

    @JvmStatic
    fun max(number: Float, max: Float): Float {
        return if (number > max) {
            max
        } else {
            number
        }
    }

    @JvmStatic
    fun min(number: Float, min: Float): Float {
        return if (number < min) {
            min
        } else {
            number
        }
    }

    @JvmStatic
    fun max(number: Int, max: Int): Int {
        return if (number > max) {
            max
        } else {
            number
        }
    }

    @JvmStatic
    fun min(number: Int, min: Int): Int {
        return if (number < min) {
            min
        } else {
            number
        }
    }


    /**Methods from libGDX - https://github.com/libgdx/libgdx
     */
    /** Returns the largest integer less than or equal to the specified float. This method will only properly floor floats from
     * -(2^14) to (Float.MAX_VALUE - 2^14).  */
    @JvmStatic
    fun floor(value: Float): Int {
        return (value + BIG_ENOUGH_FLOOR).toInt() - BIG_ENOUGH_INT
    }

    /** Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil floats from
     * -(2^14) to (Float.MAX_VALUE - 2^14).  */
    @JvmStatic
    fun ceil(value: Float): Int {
        return (value + BIG_ENOUGH_CEIL).toInt() - BIG_ENOUGH_INT
    }
}
