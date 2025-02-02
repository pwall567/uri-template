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

package io.kjson.uritemplate

import io.jstuff.util.IntOutput.append2Hex
import io.kstuff.text.StringMapper.mapCharacters
import io.kstuff.text.URIStringMapper.isUnreservedForURI
import io.kstuff.text.UTF8StringMapper.encodeUTF8

class ExpressionElement(
    val variableReferences: List<VariableReference>,
    val prefix: Char?,
    val separator: Char,
    val reservedEncoding: Boolean,
    val addVariableNames: Boolean,
    val formsStyleEqualsSign: Boolean,
) : Element {

    override fun appendTo(a: Appendable, resolver: VariableResolver) {
        var continuation = false
        for (reference in variableReferences) {
            val variableName = reference.name
            resolver[variableName]?.takeUnless {
                it is Map<*, *> && it.isEmpty() || it.getIterator().let { i -> i != null && !i.hasNext() } ||
                        it::class.java.isArray && (it as Array<*>).isEmpty()
            }?.let { value ->
                if (reference.explode) {
                    if (value is Map<*, *>) {
                        for (entry in value.entries) {
                            entry.value?.let {
                                a.appendValue(entry.key.toString(), it, null, true, continuation++)
                            }
                        }
                    }
                    else {
                        val iterator = value.getIterator()
                        if (iterator != null) {
                            while (iterator.hasNext()) {
                                iterator.next()?.let {
                                    a.appendValue(variableName, it, null, addVariableNames, continuation++)
                                }
                            }
                        }
                        else
                            a.appendValue(variableName, value, reference.characterLimit, addVariableNames,
                                    continuation++)
                    }
                }
                else
                    a.appendValue(variableName, value, reference.characterLimit, addVariableNames, continuation++)
            }
        }
    }

    private fun Appendable.appendValue(
        name: String,
        value: Any,
        characterLimit: Int?,
        addNames: Boolean,
        continuation: Boolean,
    ) {
        if (continuation)
            append(separator)
        else
            prefix?.let { append(it) }
        if (addNames) {
            append(name)
            if (formsStyleEqualsSign) {
                append('=')
                appendStringValue(value, reservedEncoding, characterLimit)
            }
            else {
                if (!(value is CharSequence && value.isEmpty())) {
                    val sb = StringBuilder()
                    sb.appendStringValue(value, reservedEncoding, characterLimit)
                    if (sb.isNotEmpty()) {
                        append('=')
                        append(sb)
                    }
                }
            }
        }
        else
            appendStringValue(value, reservedEncoding, characterLimit)
    }

    override fun toString(): String = buildString {
        append('{')
        if (prefix == null) {
            if (reservedEncoding)
                append('+')
        }
        else
            append(prefix)
        for (i in variableReferences.indices) {
            if (i > 0)
                append(',')
            append(variableReferences[i])
        }
        append('}')
    }

    companion object {

        fun Any.getIterator(): Iterator<*>? = when {
            this is Iterator<*> -> this
            this is Iterable<*> -> iterator()
            this is Map<*, *> -> entries.iterator()
            this is Pair<*, *> -> PairIterator(first, second)
            this is Triple<*, *, *> -> TripleIterator(first, second, third)
            this is Map.Entry<*, *> -> PairIterator(key, value)
            this::class.java.isArray -> (this as Array<*>).iterator()
            else -> null
        }

        internal fun Appendable.appendStringValue(obj: Any, reservedEncoding: Boolean, characterLimit: Int?) {
            when (obj) {
                is String -> append(obj.applyCharacterLimit(characterLimit).encodeUTF8().encode(reservedEncoding))
                is Map<*, *> -> {
                    var continuation = false
                    for (entry in obj.entries) {
                        entry.key?.let {
                            if (continuation++)
                                append(',')
                            appendStringValue(it, reservedEncoding, null)
                        }
                        entry.value?.let {
                            if (continuation++)
                                append(',')
                            appendStringValue(it, reservedEncoding, null)
                        }
                    }
                }
                else -> {
                    val iterator = obj.getIterator()
                    if (iterator != null) {
                        var continuation = false
                        while (iterator.hasNext()) {
                            val item = iterator.next()
                            if (item != null) {
                                if (continuation++)
                                    append(',')
                                appendStringValue(item, reservedEncoding, null)
                            }
                        }
                    } else
                        append(obj.toString().encodeUTF8().encode(reservedEncoding))
                }
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

        private fun String.encode(reservedEncoding: Boolean): String =
                if (reservedEncoding) encodeReserved() else encodeSimple()

        fun String.encodeSimple(): String = mapCharacters {
            if (it.isUnreservedForURI()) null else percentEncoded(it.code)
        }

        private fun String.encodeReserved(): String = mapCharacters {
            if (it.isUnreservedForURI() || it.isReserved()) null else percentEncoded(it.code)
        }

        private fun percentEncoded(code: Int): CharSequence = StringBuilder(3).apply {
            append('%')
            append2Hex(this, code)
        }

        private fun Char.isReserved(): Boolean = this in ":/?#[]@!$&'()*+,;="

    }

    class PairIterator(private val first: Any?, private val second: Any?) : Iterator<Any?> {

        private var index = 0

        override fun hasNext(): Boolean = index < 2

        override fun next(): Any? = when (index++) {
            0 -> first
            1 -> second
            else -> throw NoSuchElementException()
        }

    }

    class TripleIterator(private val first: Any?, private val second: Any?, private val third: Any?) : Iterator<Any?> {

        private var index = 0

        override fun hasNext(): Boolean = index < 3

        override fun next(): Any? = when (index++) {
            0 -> first
            1 -> second
            2 -> third
            else -> throw NoSuchElementException()
        }

    }

}
