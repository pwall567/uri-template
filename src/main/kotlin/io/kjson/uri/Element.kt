/*
 * @(#) Element.kt
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

package io.kjson.uri

import net.pwall.text.StringMapper.mapCharacters
import net.pwall.text.URIStringMapper.isUnreservedForURI
import net.pwall.util.IntOutput.append2Hex

sealed interface Element {

    fun appendTo(a: Appendable)

    companion object {

        fun String.encodeSimple(): String = mapCharacters {
            if (it.isUnreservedForURI()) null else StringBuilder(3).apply {
                append('%')
                append2Hex(this, it.code)
            }
        }

        fun String.encodeReserved(): String = mapCharacters {
            if (it.isUnreservedForURI() || it.isReserved()) null else StringBuilder(3).apply {
                append('%')
                append2Hex(this, it.code)
            }
        }

        private fun Char.isReserved(): Boolean = this in ":/?#[]@!$&'()*+,;="

    }

}
