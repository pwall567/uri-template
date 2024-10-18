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
    val variableReferences: List<VariableReference>,
    val prefix: Char?,
    val separator: Char,
    val reservedEncoding: Boolean,
    val addVariableNames: Boolean,
    val formsStyleEqualsSign: Boolean,
) : Element {

    override fun appendTo(a: Appendable) {
        var continuation = false
        for (reference in variableReferences) {
            val variable = reference.variable
            variable.value?.takeUnless {
                it is List<*> && it.isEmpty() || it is Map<*, *> && it.isEmpty()
            }?.let { value ->
                if (reference.explode) {
                    when (value) {
                        is List<*> -> for (item in value) {
                            item?.let { appendValue(a, variable.name, it, null, addVariableNames, continuation) }
                            continuation = true
                        }
                        is Map<*, *> -> {
                            for (entry in value.entries) {
                                entry.value?.let { appendValue(a, entry.key.toString(), it, null, true, continuation) }
                                continuation = true
                            }
                        }
                        else -> appendValue(a, variable.name, value, reference.characterLimit, addVariableNames,
                                continuation)
                    }
                }
                else
                    appendValue(a, variable.name, value, reference.characterLimit, addVariableNames, continuation)
                continuation = true
            }
        }
    }

    private fun appendValue(
        a: Appendable,
        name: String,
        value: Any?,
        characterLimit: Int?,
        addNames: Boolean,
        continuation: Boolean,
    ) {
        if (continuation)
            a.append(separator)
        else
            prefix?.let { a.append(it) }
        if (addNames) {
            a.append(name)
            if (formsStyleEqualsSign || value != null && !value.isEmptyOrAllNull())
                a.append('=')
        }
        value?.let { appendToString(a, it, characterLimit) }
    }

    private fun Any.isEmptyOrAllNull(): Boolean = when (this) {
        is CharSequence -> isEmpty()
        is List<*> -> none { it != null }
        is Map<*, *> -> entries.none { it.key != null || it.value != null }
        else -> toString().isEmpty()
    }

    private fun appendToString(a: Appendable, obj: Any, characterLimit: Int?) {
        when (obj) {
            is String -> a.append(obj.applyCharacterLimit(characterLimit).encodeUTF8().encode())
            is List<*> -> {
                var continuation = false
                for (item in obj) {
                    if (item != null) {
                        if (continuation)
                            a.append(',')
                        appendToString(a, item, null)
                        continuation = true
                    }
                }
            }
            is Map<*, *> -> {
                var continuation = false
                for (entry in obj.entries) {
                    entry.key?.let {
                        if (continuation)
                            a.append(',')
                        appendToString(a, it, null)
                        continuation = true
                    }
                    entry.value?.let {
                        if (continuation)
                            a.append(',')
                        appendToString(a, it, null)
                        continuation = true
                    }
                }
            }
            else -> a.append(obj.toString().encodeUTF8().encode())
        }
    }

    private fun String.applyCharacterLimit(characterLimit: Int?): String {
        if (characterLimit == null || length <= characterLimit)
            return this
        var size = 0
        for (i in 0 until characterLimit) {
            if (size + 1 < length && this[size].isHighSurrogate() && this[size + 1].isLowSurrogate())
                size += 2
            else
                size++
        }
        return if (size == length) this else substring(0, size)
    }

    private fun String.encode(): String = if (reservedEncoding) encodeReserved() else encodeSimple()

}
