/*
 * @(#) URITemplate.kt
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

import net.pwall.text.TextMatcher

/**
 * Kotlin implementation of the [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html) specification.
 *
 * @author  Peter Wall
 */
class URITemplate(val string: String) {

    val elements = parse(string)

    /**
     * Expand the template to a `String`, using the supplied [VariableResolver] to resolve variable names.
     */
    fun expand(resolver: VariableResolver): String = buildString {
        expandTo(this, resolver)
    }

    /**
     * Expand the template to a `String`, using the supplied [Map] to resolve variable names.
     */
    fun expand(values: Map<String, Any?>): String = buildString {
        expandTo(this) { values[it] }
    }

    /**
     * Expand the template to a `String`, using the supplied [Pair] to resolve a single variable name.
     */
    fun expand(value: Pair<String, Any?>) = buildString {
        expandTo(this) { if (it == value.first) value.second else null }
    }

    /**
     * Expand the template to a `String`, with all variables resolving to `null`.
     */
    fun expand() = buildString {
        expandTo(this) { null }
    }

    /**
     * Expand the template by appending to an [Appendable], using the supplied [VariableResolver] to resolve variable
     * names.
     */
    fun expandTo(a: Appendable, resolver: VariableResolver) {
        for (element in elements)
            element.appendTo(a, resolver)
    }

    override fun toString(): String = string

    override fun equals(other: Any?): Boolean = this === other || other is URITemplate && string == other.string

    override fun hashCode(): Int = string.hashCode()

    companion object {

        private const val RESERVED_STRING_EXPANSION = '+'
        private const val FRAGMENT_EXPANSION = '#'
        private const val LABEL_EXPANSION = '.'
        private const val PATH_SEGMENT_EXPANSION = '/'
        private const val PATH_PARAMETER_EXPANSION = ';'
        private const val QUERY_EXPANSION = '?'
        private const val QUERY_CONTINUATION_EXPANSION = '&'

        private fun parse(string: String): List<Element> {
            val elements = mutableListOf<Element>()
            val tm = TextMatcher(string)
            var textStart = 0
            while (!tm.isAtEnd) {
                when {
                    tm.match('{') -> {
                        if (tm.start > textStart)
                            elements.add(TextElement(string, textStart, tm.start))
                        elements.add(parseExpression(tm))
                        textStart = tm.index
                    }
                    tm.match('%') && tm.matchContinue(2, 2, TextMatcher::isHexDigit) -> {}
                    tm.match(::isValidTextCharacter) -> {}
                    else -> throw URITemplateException("Illegal character", tm)
                }
            }
            if (tm.index > textStart)
                elements.add(TextElement(string, textStart, tm.index))
            return elements
        }

        private fun parseExpression(tm: TextMatcher): ExpressionElement {
            val prefix: Char?
            val separator: Char
            val reservedEncoding: Boolean
            val addVariableNames: Boolean
            val formsStyleEqualsSign: Boolean
            when {
                tm.match(RESERVED_STRING_EXPANSION) -> {
                    prefix = null
                    separator = ','
                    reservedEncoding = true
                    addVariableNames = false
                    formsStyleEqualsSign = false
                }
                tm.match(FRAGMENT_EXPANSION) -> {
                    prefix = '#'
                    separator = ','
                    reservedEncoding = true
                    addVariableNames = false
                    formsStyleEqualsSign = false
                }
                tm.match(LABEL_EXPANSION) -> {
                    prefix = '.'
                    separator = '.'
                    reservedEncoding = false
                    addVariableNames = false
                    formsStyleEqualsSign = false
                }
                tm.match(PATH_SEGMENT_EXPANSION) -> {
                    prefix = '/'
                    separator = '/'
                    reservedEncoding = false
                    addVariableNames = false
                    formsStyleEqualsSign = false
                }
                tm.match(PATH_PARAMETER_EXPANSION) -> {
                    prefix = ';'
                    separator = ';'
                    reservedEncoding = false
                    addVariableNames = true
                    formsStyleEqualsSign = false
                }
                tm.match(QUERY_EXPANSION) -> {
                    prefix = '?'
                    separator = '&'
                    reservedEncoding = false
                    addVariableNames = true
                    formsStyleEqualsSign = true
                }
                tm.match(QUERY_CONTINUATION_EXPANSION) -> {
                    prefix = '&'
                    separator = '&'
                    reservedEncoding = false
                    addVariableNames = true
                    formsStyleEqualsSign = true
                }
                else -> {
                    prefix = null
                    separator = ','
                    reservedEncoding = false
                    addVariableNames = false
                    formsStyleEqualsSign = false
                }
            }
            val variableReferences = mutableListOf<VariableReference>()
            var variableStart = tm.index
            while (!tm.isAtEnd) {
                when {
                    tm.match(':') -> {
                        val variableEnd = tm.start
                        if (!tm.matchDec())
                            throw URITemplateException("Character limit colon not followed by number", tm)
                        val characterLimit = try {
                            tm.resultInt
                        } catch (_: NumberFormatException) {
                            throw URITemplateException("Illegal number (${tm.result})", tm.apply { revert() })
                        }
                        if (characterLimit >= 10000)
                            throw URITemplateException("Character limit too high ($characterLimit)",
                                    tm.apply { revert() })
                        tm.start = variableEnd
                        storeVariable(tm, variableStart, characterLimit, false, variableReferences)
                        when {
                            tm.match(',') -> variableStart = tm.index
                            tm.match('}') -> return ExpressionElement(variableReferences, prefix, separator,
                                    reservedEncoding, addVariableNames, formsStyleEqualsSign)
                            else -> throw URITemplateException("Character limit not followed by ',' or '}'", tm)
                        }
                    }
                    tm.match('*') -> {
                        storeVariable(tm, variableStart, null, true, variableReferences)
                        when {
                            tm.match(',') -> variableStart = tm.index
                            tm.match('}') -> return ExpressionElement(variableReferences, prefix, separator,
                                    reservedEncoding, addVariableNames, formsStyleEqualsSign)
                            else -> throw URITemplateException("Explode indicator not followed by ',' or '}'", tm)
                        }
                    }
                    tm.match(',') -> {
                        storeVariable(tm, variableStart, null, false, variableReferences)
                        variableStart = tm.index
                    }
                    tm.match('}') -> {
                        storeVariable(tm, variableStart, null, false, variableReferences)
                        return ExpressionElement(variableReferences, prefix, separator, reservedEncoding,
                                addVariableNames, formsStyleEqualsSign)
                    }
                    tm.match('.') -> {
                        if (tm.start == variableStart || tm.getChar(tm.start - 1) == '.')
                            throw URITemplateException("Illegal dot in variable name", tm.apply { index-- })
                    }
                    tm.match('%') && tm.matchContinue(2, 2, TextMatcher::isHexDigit) -> {}
                    tm.match(::isValidVariableCharacter) -> {}
                    else -> throw URITemplateException("Illegal character in variable", tm)
                }
            }
            throw URITemplateException("Missing end of expression (\"}\")")
        }

        private fun storeVariable(tm: TextMatcher, variableStart: Int, characterLimit: Int?, explode: Boolean,
                variableReferences: MutableList<VariableReference>) {
            if (tm.start == variableStart)
                throw URITemplateException("Variable name is empty", tm.apply { index-- })
            if (tm.getChar(tm.start - 1) == '.')
                throw URITemplateException("Illegal dot in variable name", tm.apply { index -= 2 })
            val name = tm.getString(variableStart, tm.start)
            variableReferences.add(VariableReference(name, characterLimit, explode))
        }

        private fun isValidTextCharacter(ch: Char): Boolean =
                !(ch in '\u0000'..' ' || ch in "\"%'<>\\^`|}" || ch in '\u007F'..'\u00BF')

        private fun isValidVariableCharacter(ch: Char): Boolean =
                ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '_'

    }

}
