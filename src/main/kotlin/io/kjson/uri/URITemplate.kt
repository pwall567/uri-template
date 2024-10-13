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

        fun parse(string: String): URITemplate {
            val elements = mutableListOf<Element>()
            val variables = mutableListOf<Variable>()
            val tm = TextMatcher(string)
            val sb = StringBuilder()
            while (!tm.isAtEnd) {
                when {
                    tm.match('%') -> tm.processPercent(sb)
                    tm.match('{') -> {
                        elements.addTextElement(sb)
                        elements.add(parseExpression(tm, variables))
                    }
                    tm.match(::isValidTextCharacter) -> sb.append(tm.resultChar)
                    else -> throw URITemplateException("Illegal character", tm)
                }
            }
            elements.addTextElement(sb)
            return URITemplate(elements, variables)
        }

        private fun parseExpression(tm: TextMatcher, variables: MutableList<Variable>): ExpressionElement {

            // TODO parse operator

            val sb = StringBuilder()
            while (!tm.isAtEnd) {
                when {

                    // TODO parse modifier

                    tm.match('}') -> {
                        if (sb.isEmpty())
                            throw URITemplateException("Expression is empty", tm.apply { index-- })
                        val name = sb.toString()
                        if (variables.none { it.name == name })
                            variables.add(Variable(name, null))
                        return ExpressionElement(name)
                    }
                    tm.match('%') -> tm.processPercent(sb)
                    tm.match(::isValidVariableCharacter) -> sb.append(tm.resultChar)
                    else -> throw URITemplateException("Illegal character in variable", tm)
                }
            }
            throw URITemplateException("Missing end of expression (\"}\")")
        }

        private fun MutableList<Element>.addTextElement(sb: StringBuilder) {
            if (sb.isNotEmpty()) {
                add(TextElement(sb.toString()))
                sb.setLength(0)
            }
        }

        private fun TextMatcher.processPercent(sb: StringBuilder) {
            if (matchHex(2, 2))
                sb.append('%').append(result)
            else
                throw URITemplateException("Illegal percent encoding", apply { index-- })
        }

        private fun isValidTextCharacter(ch: Char): Boolean =
                !(ch in '\u0000'..' ' || ch in "\"'<>\\^`|}" || ch in '\u007F'..'\u00BF')

        private fun isValidVariableCharacter(ch: Char): Boolean =
                ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '_'

    }

}
