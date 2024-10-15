/*
 * @(#) ExpressionElement.kt
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

import io.kjson.uri.Element.Companion.encodeReserved
import io.kjson.uri.Element.Companion.encodeSimple
import net.pwall.text.UTF8StringMapper.encodeUTF8

class ExpressionElement(
    val names: List<String>,
    val prefix: Char?,
    val separator: Char,
    val reservedEncoding: Boolean,
    val addVariableNames: Boolean = false,
    val formsStyleEqualsSign: Boolean = false,
) : Element {

    override fun appendTo(a: Appendable, variables: List<Variable>) {
        var continuation = false
        for (name in names) {
            variables.find { it.name == name }?.value?.let { value ->
                if (continuation)
                    a.append(separator)
                else
                    prefix?.let { a.append(it) }
                val text = value.toString()
                if (addVariableNames) {
                    a.append(name)
                    if (formsStyleEqualsSign || text.isNotEmpty())
                        a.append('=')
                }
                a.append(text.encodeUTF8().encode())
                continuation = true
            }
        }
    }

    private fun String.encode(): String = if (reservedEncoding) encodeReserved() else encodeSimple()

}
