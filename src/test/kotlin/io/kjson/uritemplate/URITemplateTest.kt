/*
 * @(#) URITemplateTest.kt
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect

import io.kjson.uritemplate.ExpressionElement.Companion.encodeSimple
import net.pwall.text.UTF8StringMapper.encodeUTF8

class URITemplateTest {

    @Test fun `should create simple template`() {
        val uriTemplate = URITemplate("http://kjson.io")
        with(uriTemplate) {
            expect("http://kjson.io") { string }
            with(elements) {
                expect(1) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("http://kjson.io") { text }
                    expect("http://kjson.io") { toString() }
                }
            }
        }
    }

    @Test fun `should expand simple template`() {
        shouldConvert("http://kjson.io" to "http://kjson.io")
    }

    @Test fun `should append to Appendable to assist in creating larger strings`() {
        val uriTemplate = URITemplate("http://kjson.io")
        val aElement = buildString {
            append("<a href=\"")
            uriTemplate.expandTo(this) { null }
            append("\">kjson</a>")
        }
        expect("<a href=\"http://kjson.io\">kjson</a>") { aElement }
    }

    @Test fun `should create empty template`() {
        val uriTemplate = URITemplate("")
        assertTrue(uriTemplate.elements.isEmpty())
    }

    @Test fun `should expand empty template`() {
        shouldConvert("" to "")
    }

    @Test fun `should throw exception on illegal character`() {
        assertFailsWith<URITemplateException> { URITemplate("Test<>") }.let {
            expect("Illegal character at offset 4") { it.message }
            expect("Illegal character") { it.text }
            expect(4) {it.tm?.index }
        }
    }

    @Test fun `should create simple template with percent encoded characters`() {
        val uriTemplate = URITemplate("Text-%22%25%22")
        with(uriTemplate) {
            with(elements) {
                expect(1) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("Text-%22%25%22") { text }
                    expect("Text-%22%25%22") { toString() }
                }
            }
        }
    }

    @Test fun `should expand simple template with percent encoded characters`() {
        shouldConvert("Text-%22%25%22" to "Text-%22%25%22")
    }

    @Test fun `should throw exception on illegal percent encoded character`() {
        assertFailsWith<URITemplateException> { URITemplate("Test%%") }.let {
            expect("Illegal character at offset 4") { it.message }
        }
    }

    @Test fun `should create template with variable`() {
        val templateString = "(prefix){var}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(0) { start }
                    expect(8) { end }
                    expect("(prefix)") { toString() }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                            expect("var") { toString() }
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                    expect("{var}") { toString() }
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(13) { start }
                    expect(21) { end }
                    expect("(suffix)") { toString() }
                }
            }
        }
    }

    @Test fun `should expand template with variable`() {
        val uriTemplate = URITemplate("(prefix){var}(suffix)")
        expect("(prefix)value(suffix)") { uriTemplate.expand { if (it == "var") "value" else null } }
    }

    @Test fun `should expand template with variable using Map`() {
        val uriTemplate = URITemplate("(prefix){var}(suffix)")
        expect("(prefix)value(suffix)") { uriTemplate.expand(mapOf("var" to "value")) }
    }

    @Test fun `should expand template with variable using Pair`() {
        val uriTemplate = URITemplate("(prefix){var}(suffix)")
        expect("(prefix)value(suffix)") { uriTemplate.expand("var" to "value") }
    }

    @Test fun `should expand template with unset variable`() {
        shouldConvert("(prefix){var}(suffix)" to "(prefix)(suffix)")
    }

    @Test fun `should expand template with percent encoding`() {
        shouldConvert("(prefix){var}(suffix)" to "(prefix)%3C%20%3E(suffix)", mapOf("var" to "< >"))
    }

    @Test fun `should allow percent encoding in variable names`() {
        val templateString = "(prefix){var%5F1}*{var_1}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate.elements) {
            expect(5) { size }
            with(this[0]) {
                assertIs<TextElement>(this)
                expect(templateString) { text }
                expect(0) { start }
                expect(8) { end }
                expect("(prefix)") { toString() }
            }
            with(this[1]) {
                assertIs<ExpressionElement>(this)
                with(variableReferences) {
                    expect(1) { size }
                    expect("var%5F1") { this[0].name }
                }
                expect("{var%5F1}") { toString() }
            }
            with(this[2]) {
                assertIs<TextElement>(this)
                expect(templateString) { text }
                expect(17) { start }
                expect(18) { end }
                expect("*") { toString() }
            }
            with(this[3]) {
                assertIs<ExpressionElement>(this)
                with(variableReferences) {
                    expect(1) { size }
                    expect("var_1") { this[0].name }
                }
                expect("{var_1}") { toString() }
            }
            with(this[4]) {
                assertIs<TextElement>(this)
                expect(templateString) { text }
                expect(25) { start }
                expect(33) { end }
                expect("(suffix)") { toString() }
            }
        }
    }

    @Test fun `should allow dots in variable names`() {
        val uriTemplate = URITemplate("{a.b}")
        with(uriTemplate.elements) {
            expect(1) { size }
            with(this[0]) {
                assertIs<ExpressionElement>(this)
                with(variableReferences) {
                    expect(1) { size }
                    expect("a.b") { this[0].name }
                }
                expect("{a.b}") { toString() }
            }
        }
    }

    @Test fun `should not allow dots in incorrect positions in variable names`() {
        assertFailsWith<URITemplateException> { URITemplate("{x,.a}") }.let {
            expect("Illegal dot in variable name at offset 3") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate("{a..b}") }.let {
            expect("Illegal dot in variable name at offset 3") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate("{a.}") }.let {
            expect("Illegal dot in variable name at offset 2") { it.message }
        }
    }

    @Test fun `should throw exception on illegal character in variable name`() {
        assertFailsWith<URITemplateException> { URITemplate("(prefix){var-1}(suffix)") }.let {
            expect("Illegal character in variable at offset 12") { it.message }
        }
    }

    @Test fun `should throw exception on empty expression`() {
        assertFailsWith<URITemplateException> { URITemplate("(prefix){}(suffix)") }.let {
            expect("Variable name is empty at offset 9") { it.message }
        }
    }

    @Test fun `should throw exception on expression with no closing brace`() {
        assertFailsWith<URITemplateException> { URITemplate("(prefix){var") }.let {
            expect("Missing end of expression (\"}\")") { it.message }
        }
    }

    @Test fun `should create template with reserved variable`() {
        val templateString = "(prefix){+var}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(0) { start }
                    expect(8) { end }
                    expect("(prefix)") { toString() }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertTrue(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                    expect("{+var}") { toString() }
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(14) { start }
                    expect(22) { end }
                    expect("(suffix)") { toString() }
                }
            }
        }
    }

    @Test fun `should expand template with reserved variable`() {
        shouldConvert("(prefix){+var}(suffix)" to "(prefix)/abc/def(suffix)", mapOf("var" to "/abc/def"))
    }

    @Test fun `should create template with fragment variable`() {
        val templateString = "(prefix){#var}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(0) { start }
                    expect(8) { end }
                    expect("(prefix)") { toString() }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertTrue(reservedEncoding)
                    expect('#') { prefix }
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                    expect("{#var}") { toString() }
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(14) { start }
                    expect(22) { end }
                    expect("(suffix)") { toString() }
                }
            }
        }
    }

    @Test fun `should expand template with fragment variable`() {
        shouldConvert("(prefix){#var}(suffix)" to "(prefix)#abc$(suffix)", mapOf("var" to "abc$"))
    }

    @Test fun `should create template with multiple-variable expression`() {
        val templateString = "(prefix){var1,var2}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(0) { start }
                    expect(8) { end }
                    expect("(prefix)") { toString() }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(2) { size }
                        with(this[0]) {
                            expect("var1") { name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                        with(this[1]) {
                            expect("var2") { name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                    expect("{var1,var2}") { toString() }
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(19) { start }
                    expect(27) { end }
                    expect("(suffix)") { toString() }
                }
            }
        }
    }

    @Test fun `should expand template with multiple-variable expression`() {
        shouldConvert("(prefix){var1,var2}(suffix)" to "(prefix)alpha,beta(suffix)",
                mapOf( "var1" to "alpha", "var2" to "beta"))
    }

    @Test fun `should create template with variable with character limit`() {
        val templateString = "(prefix){var:3}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(0) { start }
                    expect(8) { end }
                    expect("(prefix)") { toString() }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { name }
                            expect(3) { characterLimit }
                            assertFalse(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                    expect("{var:3}") { toString() }
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(15) { start }
                    expect(23) { end }
                    expect("(suffix)") { toString() }
                }
            }
        }
    }

    @Test fun `should throw exception on illegal character limit`() {
        assertFailsWith<URITemplateException> { URITemplate("{var:a}") }.let {
            expect("Character limit colon not followed by number at offset 5") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate("{var:99999}") }.let {
            expect("Character limit too high (99999) at offset 5") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate("{var:999999999999999}") }.let {
            expect("Illegal number (999999999999999) at offset 5") { it.message }
        }
    }

    @Test fun `should create template with variable with explode modifier`() {
        val templateString = "(prefix){var*}(suffix)"
        val uriTemplate = URITemplate(templateString)
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(0) { start }
                    expect(8) { end }
                    expect("(prefix)") { toString() }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { name }
                            assertNull(characterLimit)
                            assertTrue(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                    expect("{var*}") { toString() }
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect(templateString) { text }
                    expect(14) { start }
                    expect(22) { end }
                    expect("(suffix)") { toString() }
                }
            }
        }
    }

    @Test fun `should throw exception on illegal explode modifier`() {
        assertFailsWith<URITemplateException> { URITemplate("{var*a}") }.let {
            expect("Explode indicator not followed by ',' or '}' at offset 5") { it.message }
        }
    }

    @Test fun `should handle Unicode surrogate pairs correctly`() {
        shouldConvert("{x:2}" to "~\uD83D\uDCA9".encodeUTF8().encodeSimple(), mapOf("x" to "~\uD83D\uDCA9~"))
    }

    @Test fun `should handle nulls in lists correctly`() {
        shouldConvert("{list}" to "abc,def") { if (it == "list") listOf(null, "abc", null, "def") else null }
        shouldConvert("{/list*}" to "/abc/def") { if (it == "list") listOf(null, "abc", null, "def") else null }
    }

    @Test fun `should handle arrays correctly`() {
        shouldConvert("{array}" to "abc,def") { if (it == "array") arrayOf("abc", "def") else null }
        shouldConvert("{/array*}" to "/abc/def") { if (it == "array") arrayOf("abc", "def") else null }
        shouldConvert("{/array*}" to "/abc/def") { if (it == "array") arrayOf(null, "abc", null, "def") else null }
    }

    @Test fun `should handle range as a form of Iterable`() {
        shouldConvert("{range}" to "1,2,3,4") { if (it == "range") 1..4 else null }
        shouldConvert("{/range*}" to "/1/2/3/4") { if (it == "range") 1..4 else null }
    }

    @Test fun `should handle Pair as a form of list`() {
        shouldConvert("{pair}" to "abc,def") { if (it == "pair") "abc" to "def" else null }
        shouldConvert("{/pair*}" to "/abc/def") { if (it == "pair") "abc" to "def" else null }
    }

    @Test fun `should handle Triple as a form of list`() {
        shouldConvert("{triple}" to "abc,def,ghi") { if (it == "triple") Triple("abc", "def", "ghi") else null }
        shouldConvert("{/triple*}" to "/abc/def/ghi") { if (it == "triple") Triple("abc", "def", "ghi") else null }
    }

    @Test fun `should perform substitutions listed in specification Overview`() {
        shouldConvert("http://example.com/~{username}/" to "http://example.com/~fred/", mapOf("username" to "fred"))
        shouldConvert("http://example.com/~{username}/" to "http://example.com/~mark/", mapOf("username" to "mark"))

        shouldConvert("http://example.com/dictionary/{term:1}/{term}" to "http://example.com/dictionary/c/cat",
                mapOf("term" to "cat"))
        shouldConvert("http://example.com/dictionary/{term:1}/{term}" to "http://example.com/dictionary/d/dog",
                mapOf("term" to "dog"))

        shouldConvert("http://example.com/search{?q,lang}" to "http://example.com/search?q=cat&lang=en", mapOf(
            "q" to "cat",
            "lang" to "en",
        ))
        shouldConvert("http://example.com/search{?q,lang}" to "http://example.com/search?q=chien&lang=fr", mapOf(
            "q" to "chien",
            "lang" to "fr",
        ))

        shouldConvert(
            "http://www.example.com/foo{?query,number}" to "http://www.example.com/foo?query=mycelium&number=100",
            mapOf(
                "query" to "mycelium",
                "number" to 100,
            )
        )
        shouldConvert("http://www.example.com/foo{?query,number}" to "http://www.example.com/foo?number=100", mapOf(
            "number" to 100,
        ))
        shouldConvert("http://www.example.com/foo{?query,number}" to "http://www.example.com/foo")
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 1`() {
        shouldConvert("{var}" to "value", parameterValueMap)
        shouldConvert("{hello}" to "Hello%20World%21", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 2`() {
        shouldConvert("{+var}" to "value", parameterValueMap)
        shouldConvert("{+hello}" to "Hello%20World!", parameterValueMap)
        shouldConvert("{+path}/here" to "/foo/bar/here", parameterValueMap)
        shouldConvert("here?ref={+path}" to "here?ref=/foo/bar", parameterValueMap)

        shouldConvert("X{#var}" to "X#value", parameterValueMap)
        shouldConvert("X{#hello}" to "X#Hello%20World!", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 3`() {
        shouldConvert("map?{x,y}" to "map?1024,768", parameterValueMap)
        shouldConvert("{x,hello,y}" to "1024,Hello%20World%21,768", parameterValueMap)

        shouldConvert("{+x,hello,y}" to "1024,Hello%20World!,768", parameterValueMap)
        shouldConvert("{+path,x}/here" to "/foo/bar,1024/here", parameterValueMap)

        shouldConvert("{#x,hello,y}" to "#1024,Hello%20World!,768", parameterValueMap)
        shouldConvert("{#path,x}/here" to "#/foo/bar,1024/here", parameterValueMap)

        shouldConvert("X{.var}" to "X.value", parameterValueMap)
        shouldConvert("X{.x,y}" to "X.1024.768", parameterValueMap)

        shouldConvert("{/var}" to "/value", parameterValueMap)
        shouldConvert("{/var,x}/here" to "/value/1024/here", parameterValueMap)

        shouldConvert("{;x,y}" to ";x=1024;y=768", parameterValueMap)
        shouldConvert("{;x,y,empty}" to ";x=1024;y=768;empty", parameterValueMap)

        shouldConvert("{?x,y}" to "?x=1024&y=768", parameterValueMap)
        shouldConvert("{?x,y,empty}" to "?x=1024&y=768&empty=", parameterValueMap)

        shouldConvert("?fixed=yes{&x}" to "?fixed=yes&x=1024", parameterValueMap)
        shouldConvert("{&x,y,empty}" to "&x=1024&y=768&empty=", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 4`() {
        shouldConvert("{var:3}" to "val", parameterValueMap)
        shouldConvert("{var:30}" to "value", parameterValueMap)
        shouldConvert("{list}" to "red,green,blue", parameterValueMap)
        shouldConvert("{list*}" to "red,green,blue", parameterValueMap)
        shouldConvert("{keys}" to "semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{keys*}" to "semi=%3B,dot=.,comma=%2C", parameterValueMap)

        shouldConvert("{+path:6}/here" to "/foo/b/here", parameterValueMap)
        shouldConvert("{+list}" to "red,green,blue", parameterValueMap)
        shouldConvert("{+list*}" to "red,green,blue", parameterValueMap)
        shouldConvert("{+keys}" to "semi,;,dot,.,comma,,", parameterValueMap)
        shouldConvert("{+keys*}" to "semi=;,dot=.,comma=,", parameterValueMap)

        shouldConvert("{#path:6}/here" to "#/foo/b/here", parameterValueMap)
        shouldConvert("{#list}" to "#red,green,blue", parameterValueMap)
        shouldConvert("{#list*}" to "#red,green,blue", parameterValueMap)
        shouldConvert("{#keys}" to "#semi,;,dot,.,comma,,", parameterValueMap)
        shouldConvert("{#keys*}" to "#semi=;,dot=.,comma=,", parameterValueMap)

        shouldConvert("X{.var:3}" to "X.val", parameterValueMap)
        shouldConvert("X{.list}" to "X.red,green,blue", parameterValueMap)
        shouldConvert("X{.list*}" to "X.red.green.blue", parameterValueMap)
        shouldConvert("X{.keys}" to "X.semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("X{.keys*}" to "X.semi=%3B.dot=..comma=%2C", parameterValueMap)

        shouldConvert("{/var:1,var}" to "/v/value", parameterValueMap)
        shouldConvert("{/list}" to "/red,green,blue", parameterValueMap)
        shouldConvert("{/list*}" to "/red/green/blue", parameterValueMap)
        shouldConvert("{/list*,path:4}" to "/red/green/blue/%2Ffoo", parameterValueMap)
        shouldConvert("{/keys}" to "/semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{/keys*}" to "/semi=%3B/dot=./comma=%2C", parameterValueMap)

        shouldConvert("{;hello:5}" to ";hello=Hello", parameterValueMap)
        shouldConvert("{;list}" to ";list=red,green,blue", parameterValueMap)
        shouldConvert("{;list*}" to ";list=red;list=green;list=blue", parameterValueMap)
        shouldConvert("{;keys}" to ";keys=semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{;keys*}" to ";semi=%3B;dot=.;comma=%2C", parameterValueMap)

        shouldConvert("{?var:3}" to "?var=val", parameterValueMap)
        shouldConvert("{?list}" to "?list=red,green,blue", parameterValueMap)
        shouldConvert("{?list*}" to "?list=red&list=green&list=blue", parameterValueMap)
        shouldConvert("{?keys}" to "?keys=semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{?keys*}" to "?semi=%3B&dot=.&comma=%2C", parameterValueMap)

        shouldConvert("{&var:3}" to "&var=val", parameterValueMap)
        shouldConvert("{&list}" to "&list=red,green,blue", parameterValueMap)
        shouldConvert("{&list*}" to "&list=red&list=green&list=blue", parameterValueMap)
        shouldConvert("{&keys}" to "&keys=semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{&keys*}" to "&semi=%3B&dot=.&comma=%2C", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Prefix Values`() {
        shouldConvert("{var}" to "value", mapOf("var" to "value"))
        shouldConvert("{var:20}" to "value", mapOf("var" to "value"))
        shouldConvert("{var:3}" to "val", mapOf("var" to "value"))
        shouldConvert("{semi}" to "%3B", mapOf("semi" to ";"))
        shouldConvert("{semi:2}" to "%3B", mapOf("semi" to ";"))
    }

    @Test fun `should perform substitutions listed in specification for Composite Values`() {
        shouldConvert("/mapper{?address*}" to "/mapper?city=Newport%20Beach&state=CA", mapOf(
            "address" to mapOf(
                "city" to "Newport Beach",
                "state" to "CA",
            )
        ))
        shouldConvert("find{?year*}" to "find?year=1965&year=2000&year=2012",
                mapOf("year" to listOf("1965", "2000", "2012")))
        shouldConvert("www{.dom*}" to "www.example.com", mapOf("dom" to listOf("example", "com")))
    }

    @Test fun `should perform substitutions listed in specification for Variable Expansion`() {
        shouldConvert("{count}" to "one,two,three", parameterValueMap)
        shouldConvert("{count*}" to "one,two,three", parameterValueMap)
        shouldConvert("{/count}" to "/one,two,three", parameterValueMap)
        shouldConvert("{/count*}" to "/one/two/three", parameterValueMap)
        shouldConvert("{;count}" to ";count=one,two,three", parameterValueMap)
        shouldConvert("{;count*}" to ";count=one;count=two;count=three", parameterValueMap)
        shouldConvert("{?count}" to "?count=one,two,three", parameterValueMap)
        shouldConvert("{?count*}" to "?count=one&count=two&count=three", parameterValueMap)
        shouldConvert("{&count*}" to "&count=one&count=two&count=three", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Simple String Expansion`() {
        shouldConvert("{var}" to "value", parameterValueMap)
        shouldConvert("{hello}" to "Hello%20World%21", parameterValueMap)
        shouldConvert("{half}" to "50%25", parameterValueMap)
        shouldConvert("O{empty}X" to "OX", parameterValueMap)
        shouldConvert("O{undef}X" to "OX", parameterValueMap)
        shouldConvert("{x,y}" to "1024,768", parameterValueMap)
        shouldConvert("{x,hello,y}" to "1024,Hello%20World%21,768", parameterValueMap)
        shouldConvert("?{x,empty}" to "?1024,", parameterValueMap)
        shouldConvert("?{x,undef}" to "?1024", parameterValueMap)
        shouldConvert("?{undef,y}" to "?768", parameterValueMap)
        shouldConvert("{var:3}" to "val", parameterValueMap)
        shouldConvert("{var:30}" to "value", parameterValueMap)
        shouldConvert("{list}" to "red,green,blue", parameterValueMap)
        shouldConvert("{list*}" to "red,green,blue", parameterValueMap)
        shouldConvert("{keys}" to "semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{keys*}" to "semi=%3B,dot=.,comma=%2C", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Reserved Expansion`() {
        shouldConvert("{+var}" to "value", parameterValueMap)
        shouldConvert("{+hello}" to "Hello%20World!", parameterValueMap)
        shouldConvert("{+half}" to "50%25", parameterValueMap)

        shouldConvert("{base}index" to "http%3A%2F%2Fexample.com%2Fhome%2Findex", parameterValueMap)
        shouldConvert("{+base}index" to "http://example.com/home/index", parameterValueMap)
        shouldConvert("O{+empty}X" to "OX", parameterValueMap)
        shouldConvert("O{+undef}X" to "OX", parameterValueMap)

        shouldConvert("{+path}/here" to "/foo/bar/here", parameterValueMap)
        shouldConvert("here?ref={+path}" to "here?ref=/foo/bar", parameterValueMap)
        shouldConvert("up{+path}{var}/here" to "up/foo/barvalue/here", parameterValueMap)
        shouldConvert("{+x,hello,y}" to "1024,Hello%20World!,768", parameterValueMap)
        shouldConvert("{+path,x}/here" to "/foo/bar,1024/here", parameterValueMap)

        shouldConvert("{+path:6}/here" to "/foo/b/here", parameterValueMap)
        shouldConvert("{+list}" to "red,green,blue", parameterValueMap)
        shouldConvert("{+list*}" to "red,green,blue", parameterValueMap)
        shouldConvert("{+keys}" to "semi,;,dot,.,comma,,", parameterValueMap)
        shouldConvert("{+keys*}" to "semi=;,dot=.,comma=,", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Fragment Expansion`() {
        shouldConvert("{#var}" to "#value", parameterValueMap)
        shouldConvert("{#hello}" to "#Hello%20World!", parameterValueMap)
        shouldConvert("{#half}" to "#50%25", parameterValueMap)
        shouldConvert("foo{#empty}" to "foo#", parameterValueMap)
        shouldConvert("foo{#undef}" to "foo", parameterValueMap)
        shouldConvert("{#x,hello,y}" to "#1024,Hello%20World!,768", parameterValueMap)
        shouldConvert("{#path,x}/here" to "#/foo/bar,1024/here", parameterValueMap)
        shouldConvert("{#path:6}/here" to "#/foo/b/here", parameterValueMap)
        shouldConvert("{#list}" to "#red,green,blue", parameterValueMap)
        shouldConvert("{#list*}" to "#red,green,blue", parameterValueMap)
        shouldConvert("{#keys}" to "#semi,;,dot,.,comma,,", parameterValueMap)
        shouldConvert("{#keys*}" to "#semi=;,dot=.,comma=,", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Label Expansion with Dot-Prefix`() {
        shouldConvert("{.who}" to ".fred", parameterValueMap)
        shouldConvert("{.who,who}" to ".fred.fred", parameterValueMap)
        shouldConvert("{.half,who}" to ".50%25.fred", parameterValueMap)
        shouldConvert("www{.dom*}" to "www.example.com", parameterValueMap)
        shouldConvert("X{.var}" to "X.value", parameterValueMap)
        shouldConvert("X{.empty}" to "X.", parameterValueMap)
        shouldConvert("X{.undef}" to "X", parameterValueMap)
        shouldConvert("X{.var:3}" to "X.val", parameterValueMap)
        shouldConvert("X{.list}" to "X.red,green,blue", parameterValueMap)
        shouldConvert("X{.list*}" to "X.red.green.blue", parameterValueMap)
        shouldConvert("X{.keys}" to "X.semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("X{.keys*}" to "X.semi=%3B.dot=..comma=%2C", parameterValueMap)
        shouldConvert("X{.empty_keys}" to "X", parameterValueMap)
        shouldConvert("X{.empty_keys*}" to "X", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Path Segment Expansion`() {
        shouldConvert("{/who}" to "/fred", parameterValueMap)
        shouldConvert("{/who,who}" to "/fred/fred", parameterValueMap)
        shouldConvert("{/half,who}" to "/50%25/fred", parameterValueMap)
        shouldConvert("{/var}" to "/value", parameterValueMap)
        shouldConvert("{/var,empty}" to "/value/", parameterValueMap)
        shouldConvert("{/var,undef}" to "/value", parameterValueMap)
        shouldConvert("{/var,x}/here" to "/value/1024/here", parameterValueMap)
        shouldConvert("{/var:1,var}" to "/v/value", parameterValueMap)
        shouldConvert("{/list}" to "/red,green,blue", parameterValueMap)
        shouldConvert("{/list*}" to "/red/green/blue", parameterValueMap)
        shouldConvert("{/list*,path:4}" to "/red/green/blue/%2Ffoo", parameterValueMap)
        shouldConvert("{/keys}" to "/semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{/keys*}" to "/semi=%3B/dot=./comma=%2C", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Path-Style Parameter Expansion`() {
        shouldConvert("{;who}" to ";who=fred", parameterValueMap)
        shouldConvert("{;half}" to ";half=50%25", parameterValueMap)
        shouldConvert("{;empty}" to ";empty", parameterValueMap)
        shouldConvert("{;v,empty,who}" to ";v=6;empty;who=fred", parameterValueMap)
        shouldConvert("{;v,bar,who}" to ";v=6;who=fred", parameterValueMap)
        shouldConvert("{;x,y}" to ";x=1024;y=768", parameterValueMap)
        shouldConvert("{;x,y,empty}" to ";x=1024;y=768;empty", parameterValueMap)
        shouldConvert("{;x,y,undef}" to ";x=1024;y=768", parameterValueMap)
        shouldConvert("{;hello:5}" to ";hello=Hello", parameterValueMap)
        shouldConvert("{;list}" to ";list=red,green,blue", parameterValueMap)
        shouldConvert("{;list*}" to ";list=red;list=green;list=blue", parameterValueMap)
        shouldConvert("{;keys}" to ";keys=semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{;keys*}" to ";semi=%3B;dot=.;comma=%2C", parameterValueMap)
        shouldConvert("XXXX" to "XXXX", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Form-Style Query Expansion`() {
        shouldConvert("{?who}" to "?who=fred", parameterValueMap)
        shouldConvert("{?half}" to "?half=50%25", parameterValueMap)
        shouldConvert("{?x,y}" to "?x=1024&y=768", parameterValueMap)
        shouldConvert("{?x,y,empty}" to "?x=1024&y=768&empty=", parameterValueMap)
        shouldConvert("{?x,y,undef}" to "?x=1024&y=768", parameterValueMap)
        shouldConvert("{?var:3}" to "?var=val", parameterValueMap)
        shouldConvert("{?list}" to "?list=red,green,blue", parameterValueMap)
        shouldConvert("{?list*}" to "?list=red&list=green&list=blue", parameterValueMap)
        shouldConvert("{?keys}" to "?keys=semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{?keys*}" to "?semi=%3B&dot=.&comma=%2C", parameterValueMap)
    }

    @Test fun `should perform substitutions listed in specification for Form-Style Query Continuation`() {
        shouldConvert("{&who}" to "&who=fred", parameterValueMap)
        shouldConvert("{&half}" to "&half=50%25", parameterValueMap)
        shouldConvert("?fixed=yes{&x}" to "?fixed=yes&x=1024", parameterValueMap)
        shouldConvert("{&x,y,empty}" to "&x=1024&y=768&empty=", parameterValueMap)
        shouldConvert("{&x,y,undef}" to "&x=1024&y=768", parameterValueMap)

        shouldConvert("{&var:3}" to "&var=val", parameterValueMap)
        shouldConvert("{&list}" to "&list=red,green,blue", parameterValueMap)
        shouldConvert("{&list*}" to "&list=red&list=green&list=blue", parameterValueMap)
        shouldConvert("{&keys}" to "&keys=semi,%3B,dot,.,comma,%2C", parameterValueMap)
        shouldConvert("{&keys*}" to "&semi=%3B&dot=.&comma=%2C", parameterValueMap)
        shouldConvert("{&keys*}" to "&semi=%3B&dot=.&comma=%2C", parameterValueMap)
    }

    private fun shouldConvert(pair: Pair<String, String>) {
        val uriTemplate = URITemplate(pair.first)
        expect(pair.second) { uriTemplate.expand() }
    }

    private fun shouldConvert(pair: Pair<String, String>, resolver: VariableResolver) {
        val uriTemplate = URITemplate(pair.first)
        expect(pair.second) { uriTemplate.expand(resolver) }
    }

    private fun shouldConvert(pair: Pair<String, String>, values: Map<String, Any?>) {
        val uriTemplate = URITemplate(pair.first)
        expect(pair.second) { uriTemplate.expand(values) }
    }

    private val parameterValueMap = mapOf(
        "count" to listOf("one", "two", "three"),
        "dom" to listOf("example", "com"),
        "dub" to "me/too",
        "hello" to "Hello World!",
        "half" to "50%",
        "var" to "value",
        "who" to "fred",
        "base" to "http://example.com/home/",
        "path" to "/foo/bar",
        "list" to listOf("red", "green", "blue"),
        "keys" to mapOf("semi" to ";", "dot" to ".", "comma" to ","),
        "v" to "6",
        "x" to "1024",
        "y" to "768",
        "empty" to "",
        "empty_keys" to emptyList<String>(),
        "undef" to null,
    )

}
