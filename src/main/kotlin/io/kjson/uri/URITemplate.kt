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

package io.kjson.uri

import net.pwall.text.TextMatcher

/**
 * Kotlin implementation of the [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html) specification.
 *
 * @author  Peter Wall
 */
class URITemplate private constructor(val elements: List<Element>, val variables: List<Variable>) {

    override fun toString(): String = buildString {
        for (element in elements)
            element.appendTo(this, variables)
    }

    operator fun set(name: String, value: Any?) {
        getVariable(name).value = value
    }

    operator fun get(name: String): Any? = getVariable(name).value

    operator fun contains(name: String): Boolean = variables.any { it.name == name }

    fun copy(): URITemplate = URITemplate(elements, variables)

    fun clear() {
        for (variable in variables)
            variable.value = null
    }

    private fun getVariable(name: String): Variable = variables.find { it.name == name } ?:
            throw URITemplateException("Variable not recognised - $name")

    companion object {

        private const val RESERVED_STRING_EXPANSION = '+'
        private const val FRAGMENT_EXPANSION = '#'
        private const val LABEL_EXPANSION = '.'
        private const val PATH_SEGMENT_EXPANSION = '/'
        private const val PATH_PARAMETER_EXPANSION = ';'
        private const val QUERY_EXPANSION = '?'
        private const val QUERY_CONTINUATION_EXPANSION = '&'

        fun parse(string: String): URITemplate {
            val elements = mutableListOf<Element>()
            val variables = mutableListOf<Variable>()
            val tm = TextMatcher(string)
            var textStart = 0
            while (!tm.isAtEnd) {
                when {
                    tm.match('{') -> {
                        if (tm.start > textStart)
                            elements.add(TextElement(tm.getString(textStart, tm.start)))
                        elements.add(parseExpression(tm, variables))
                        textStart = tm.index
                    }
                    tm.match('%') && tm.matchContinue(2, 2, TextMatcher::isHexDigit) -> {}
                    tm.match(::isValidTextCharacter) -> {}
                    else -> throw URITemplateException("Illegal character", tm)
                }
            }
            if (tm.index > textStart)
                elements.add(TextElement(tm.getString(textStart, tm.index)))
            return URITemplate(elements, variables)
        }

        private fun parseExpression(tm: TextMatcher, variables: MutableList<Variable>): ExpressionElement {
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
            val names = mutableListOf<String>()
            var variableStart = tm.index
            while (!tm.isAtEnd) {
                when {

                    // TODO parse modifier

                    tm.match(',') -> {
                        storeVariable(tm, variableStart, variables, names)
                        variableStart = tm.index
                    }
                    tm.match('}') -> {
                        storeVariable(tm, variableStart, variables, names)
                        return ExpressionElement(names, prefix, separator, reservedEncoding, addVariableNames,
                                formsStyleEqualsSign)
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

        private fun storeVariable(tm: TextMatcher, variableStart: Int, variables: MutableList<Variable>,
                names: MutableList<String>) {
            if (tm.start == variableStart)
                throw URITemplateException("Variable name is empty", tm.apply { index-- })
            if (tm.getChar(tm.start - 1) == '.')
                throw URITemplateException("Illegal dot in variable name", tm.apply { index -= 2 })
            val name = tm.getString(variableStart, tm.start)
            if (variables.none { it.name == name })
                variables.add(Variable(name, null))
            names.add(name)
        }

        private fun isValidTextCharacter(ch: Char): Boolean =
                !(ch in '\u0000'..' ' || ch in "\"%'<>\\^`|}" || ch in '\u007F'..'\u00BF')

        private fun isValidVariableCharacter(ch: Char): Boolean =
                ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '_'

    }

}
