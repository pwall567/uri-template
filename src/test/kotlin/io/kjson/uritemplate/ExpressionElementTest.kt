/*
 * @(#) ExpressionElementTest.kt
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.uritemplate.ExpressionElement.Companion.getIterator

class ExpressionElementTest {

    @Test fun `should return iterator for List`() {
        val list = listOf("cat", "dog", "horse")
        val iterator = list.getIterator()
        assertNotNull(iterator)
        assertTrue(iterator.hasNext())
        expect("cat") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("dog") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("horse") { iterator.next() }
        assertFalse(iterator.hasNext())
    }

    @Test fun `should return iterator for Map`() {
        val map = mapOf("Fred" to "cat", "Mary" to "dog", "Janet" to "horse")
        // relies on mapOf using LinkedHashMap to retain order
        val iterator = map.getIterator()
        assertNotNull(iterator)
        assertTrue(iterator.hasNext())
        with(iterator.next()) {
            assertIs<Map.Entry<*, *>>(this)
            expect("Fred") { key }
            expect("cat") { value }
        }
        assertTrue(iterator.hasNext())
        with(iterator.next()) {
            assertIs<Map.Entry<*, *>>(this)
            expect("Mary") { key }
            expect("dog") { value }
        }
        assertTrue(iterator.hasNext())
        with(iterator.next()) {
            assertIs<Map.Entry<*, *>>(this)
            expect("Janet") { key }
            expect("horse") { value }
        }
        assertFalse(iterator.hasNext())
    }

    @Test fun `should return iterator for Pair`() {
        val pair = "sheep" to "goat"
        val iterator = pair.getIterator()
        assertNotNull(iterator)
        assertTrue(iterator.hasNext())
        expect("sheep") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("goat") { iterator.next() }
        assertFalse(iterator.hasNext())
    }

    @Test fun `should return iterator for Triple`() {
        val triple = Triple("sheep", "goat", "pig")
        val iterator = triple.getIterator()
        assertNotNull(iterator)
        assertTrue(iterator.hasNext())
        expect("sheep") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("goat") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("pig") { iterator.next() }
        assertFalse(iterator.hasNext())
    }

    @Test fun `should return iterator for Map Entry`() {
        val map = mapOf("Fred" to "cat")
        val iterator = map.entries.first().getIterator()
        assertNotNull(iterator)
        assertTrue(iterator.hasNext())
        expect("Fred") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("cat") { iterator.next() }
        assertFalse(iterator.hasNext())
    }

    @Test fun `should return iterator for Array`() {
        val array = arrayOf("cat", "dog", "horse")
        val iterator = array.getIterator()
        assertNotNull(iterator)
        assertTrue(iterator.hasNext())
        expect("cat") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("dog") { iterator.next() }
        assertTrue(iterator.hasNext())
        expect("horse") { iterator.next() }
        assertFalse(iterator.hasNext())
    }

    @Test fun `should return null iterator for String`() {
        val string = "should return null"
        assertNull(string.getIterator())
    }

}
