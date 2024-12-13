/*
 * @(#) BooleanOpsTest.kt
 *
 * uri-template  Kotlin implementation of URI Template
 * Copyright (c) 2024 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kjson.uritemplate

import kotlin.test.Test

import io.kstuff.test.shouldBe

class BooleanOpsTest {

    @Test fun `should set Boolean to true after testing`() {
        var booleanValue = false
        @Suppress("KotlinConstantConditions")
        booleanValue++ shouldBe false
        booleanValue shouldBe true
        booleanValue++ shouldBe true
        booleanValue shouldBe true
    }

    @Test fun `should set Boolean to true before testing`() {
        var booleanValue = false
        @Suppress("KotlinConstantConditions")
        ++booleanValue shouldBe true
        booleanValue shouldBe true
        ++booleanValue shouldBe true
        booleanValue shouldBe true
    }

    @Test fun `should set Boolean to false after testing`() {
        var booleanValue = true
        @Suppress("KotlinConstantConditions")
        booleanValue-- shouldBe true
        booleanValue shouldBe false
        booleanValue-- shouldBe false
        booleanValue shouldBe false
    }

    @Test fun `should set Boolean to false before testing`() {
        var booleanValue = true
        @Suppress("KotlinConstantConditions")
        --booleanValue shouldBe false
        booleanValue shouldBe false
        --booleanValue shouldBe false
        booleanValue shouldBe false
    }

}
