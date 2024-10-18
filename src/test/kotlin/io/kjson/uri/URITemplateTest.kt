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

package io.kjson.uri

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.expect
import io.kjson.uri.Element.Companion.encodeSimple
import net.pwall.text.UTF8StringMapper.encodeUTF8

class URITemplateTest {

    @Test fun `should create simple template`() {
        val uriTemplate = URITemplate.parse("http://kjson.io")
        with(uriTemplate) {
            with(elements) {
                expect(1) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("http://kjson.io") { text }
                }
            }
        }
    }

    @Test fun `should create simple template using function`() {
        val uriTemplate = URITemplate("http://kjson.io")
        with(uriTemplate) {
            with(elements) {
                expect(1) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("http://kjson.io") { text }
                }
            }
        }
    }

    @Test fun `should expand simple template`() {
        shouldConvert("http://kjson.io" to "http://kjson.io")
    }

    @Test fun `should append to Appendable to assist in creating larger strings`() {
        val uriTemplate = URITemplate.parse("http://kjson.io")
        val aElement = buildString {
            append("<a href=\"")
            uriTemplate.appendTo(this)
            append("\">kjson</a>")
        }
        expect("<a href=\"http://kjson.io\">kjson</a>") { aElement }
    }

    @Test fun `should create empty template`() {
        val uriTemplate = URITemplate.parse("")
        assertTrue(uriTemplate.elements.isEmpty())
        assertTrue(uriTemplate.variables.isEmpty())
    }

    @Test fun `should expand empty template`() {
        shouldConvert("" to "")
    }

    @Test fun `should throw exception on illegal character`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("Test<>") }.let {
            expect("Illegal character at offset 4") { it.message }
            expect("Illegal character") { it.text }
            expect(4) {it.tm?.index }
        }
    }

    @Test fun `should create simple template with percent encoded characters`() {
        val uriTemplate = URITemplate.parse("Text-%22%25%22")
        with(uriTemplate) {
            with(elements) {
                expect(1) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("Text-%22%25%22") { text }
                }
            }
        }
    }

    @Test fun `should expand simple template with percent encoded characters`() {
        shouldConvert("Text-%22%25%22" to "Text-%22%25%22")
    }

    @Test fun `should throw exception on illegal percent encoded character`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("Test%%") }.let {
            expect("Illegal character at offset 4") { it.message }
        }
    }

    @Test fun `should create template with variable`() {
        val uriTemplate = URITemplate.parse("(prefix){var}(suffix)")
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { variable.name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(1) { size }
                expect(Variable("var", null)) { this[0] }
            }
        }
        assertTrue("var" in uriTemplate)
        assertFalse("xxx" in uriTemplate)
    }

    @Test fun `should expand template with variable`() {
        val uriTemplate = shouldConvert("(prefix){var}(suffix)" to "(prefix)value(suffix)") {
            this["var"] = "value"
        }
        expect("value") { uriTemplate["var"] }
    }

    @Test fun `should expand template with unset variable`() {
        shouldConvert("(prefix){var}(suffix)" to "(prefix)(suffix)")
    }

    @Test fun `should expand template with percent encoding`() {
        val uriTemplate = shouldConvert("(prefix){var}(suffix)" to "(prefix)%3C%20%3E(suffix)") {
            this["var"] = "< >"
        }
        expect("< >") { uriTemplate["var"] }
    }

    @Test fun `should allow percent encoding in variable names`() {
        val uriTemplate = URITemplate.parse("(prefix){var%5F1}*{var_1}(suffix)")
        with(uriTemplate.variables) {
            expect(2) { size }
            expect("var%5F1") { this[0].name }
            expect("var_1") { this[1].name }
        }
    }

    @Test fun `should allow dots in variable names`() {
        val uriTemplate = URITemplate.parse("{a.b}")
        with(uriTemplate.variables) {
            expect(1) { size }
            expect("a.b") { this[0].name }
        }
    }

    @Test fun `should not allow dots in incorrect positions in variable names`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("{x,.a}") }.let {
            expect("Illegal dot in variable name at offset 3") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate.parse("{a..b}") }.let {
            expect("Illegal dot in variable name at offset 3") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate.parse("{a.}") }.let {
            expect("Illegal dot in variable name at offset 2") { it.message }
        }
    }

    @Test fun `should create only one variable entry when variable name is repeated`() {
        val uriTemplate = URITemplate.parse("(prefix){var}(middle){var}(suffix)")
        with(uriTemplate.variables) {
            expect(1) { size }
            expect("var") { this[0].name }
        }
        uriTemplate["var"] = "hello"
        expect("(prefix)hello(middle)hello(suffix)") { uriTemplate.toString() }
        expect("hello") { uriTemplate["var"] }
    }

    @Test fun `should throw exception on illegal character in variable name`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("(prefix){var-1}(suffix)") }.let {
            expect("Illegal character in variable at offset 12") { it.message }
        }
    }

    @Test fun `should throw exception on empty expression`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("(prefix){}(suffix)") }.let {
            expect("Variable name is empty at offset 9") { it.message }
        }
    }

    @Test fun `should throw exception on expression with no closing brace`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("(prefix){var") }.let {
            expect("Missing end of expression (\"}\")") { it.message }
        }
    }

    @Test fun `should throw exception on use of incorrect variable name`() {
        val uriTemplate = URITemplate.parse("(prefix){var}(suffix)")
        assertFailsWith<URITemplateException> { uriTemplate["wrong"] = "value" }.let {
            expect("Variable not recognised - wrong") { it.message }
        }
    }

    @Test fun `should create a copy of a template`() {
        val uriTemplate1 = URITemplate.parse("(prefix){var}(suffix)")
        val uriTemplate2 = uriTemplate1.copy()
        with(uriTemplate2) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { variable.name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(1) { size }
                expect(Variable("var", null)) { this[0] }
            }
        }
    }

    @Test fun `should clear variable values`() {
        val uriTemplate = URITemplate.parse("(prefix){var}(suffix)")
        uriTemplate["var"] = "value"
        expect("(prefix)value(suffix)") { uriTemplate.toString() }
        uriTemplate.clear()
        expect("(prefix)(suffix)") { uriTemplate.toString() }
        assertNull(uriTemplate["var"])
    }

    @Test fun `should create template with reserved variable`() {
        val uriTemplate = URITemplate.parse("(prefix){+var}(suffix)")
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { variable.name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertTrue(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(1) { size }
                expect(Variable("var", null)) { this[0] }
            }
        }
        assertTrue("var" in uriTemplate)
        assertFalse("xxx" in uriTemplate)
    }

    @Test fun `should expand template with reserved variable`() {
        val uriTemplate = shouldConvert("(prefix){+var}(suffix)" to "(prefix)/abc/def(suffix)") {
            this["var"] = "/abc/def"
        }
        expect("/abc/def") { uriTemplate["var"] }
    }

    @Test fun `should create template with fragment variable`() {
        val uriTemplate = URITemplate.parse("(prefix){#var}(suffix)")
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { variable.name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertTrue(reservedEncoding)
                    expect('#') { prefix }
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(1) { size }
                expect(Variable("var", null)) { this[0] }
            }
        }
        assertTrue("var" in uriTemplate)
        assertFalse("xxx" in uriTemplate)
    }

    @Test fun `should expand template with fragment variable`() {
        val uriTemplate = shouldConvert("(prefix){#var}(suffix)" to "(prefix)#abc$(suffix)") {
            this["var"] = "abc$"
        }
        expect("abc$") { uriTemplate["var"] }
    }

    @Test fun `should create template with multiple-variable expression`() {
        val uriTemplate = URITemplate.parse("(prefix){var1,var2}(suffix)")
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(2) { size }
                        with(this[0]) {
                            expect("var1") { variable.name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                        with(this[1]) {
                            expect("var2") { variable.name }
                            assertNull(characterLimit)
                            assertFalse(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(2) { size }
                expect(Variable("var1", null)) { this[0] }
                expect(Variable("var2", null)) { this[1] }
            }
        }
        assertTrue("var1" in uriTemplate)
        assertTrue("var2" in uriTemplate)
        assertFalse("xxx" in uriTemplate)
    }

    @Test fun `should expand template with multiple-variable expression`() {
        val uriTemplate = shouldConvert("(prefix){var1,var2}(suffix)" to "(prefix)alpha,beta(suffix)") {
            this["var1"] = "alpha"
            this["var2"] = "beta"
        }
        expect("alpha") { uriTemplate["var1"] }
        expect("beta") { uriTemplate["var2"] }
    }

    @Test fun `should create template with variable with character limit`() {
        val uriTemplate = URITemplate.parse("(prefix){var:3}(suffix)")
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { variable.name }
                            expect(3) { characterLimit }
                            assertFalse(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(1) { size }
                expect(Variable("var", null)) { this[0] }
            }
        }
        assertTrue("var" in uriTemplate)
        assertFalse("xxx" in uriTemplate)
    }

    @Test fun `should throw exception on illegal character limit`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("{var:a}") }.let {
            expect("Character limit colon not followed by number at offset 5") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate.parse("{var:99999}") }.let {
            expect("Character limit too high (99999) at offset 5") { it.message }
        }
        assertFailsWith<URITemplateException> { URITemplate.parse("{var:999999999999999}") }.let {
            expect("Illegal number (999999999999999) at offset 5") { it.message }
        }
    }

    @Test fun `should create template with variable with explode modifier`() {
        val uriTemplate = URITemplate.parse("(prefix){var*}(suffix)")
        with(uriTemplate) {
            with(elements) {
                expect(3) { size }
                with(this[0]) {
                    assertIs<TextElement>(this)
                    expect("(prefix)") { text }
                }
                with(this[1]) {
                    assertIs<ExpressionElement>(this)
                    with(variableReferences) {
                        expect(1) { size }
                        with(this[0]) {
                            expect("var") { variable.name }
                            assertNull(characterLimit)
                            assertTrue(explode)
                        }
                    }
                    assertFalse(reservedEncoding)
                    assertNull(prefix)
                    expect(',') { separator }
                    assertFalse(addVariableNames)
                    assertFalse(formsStyleEqualsSign)
                }
                with(this[2]) {
                    assertIs<TextElement>(this)
                    expect("(suffix)") { text }
                }
            }
            with(variables) {
                expect(1) { size }
                expect(Variable("var", null)) { this[0] }
            }
        }
        assertTrue("var" in uriTemplate)
        assertFalse("xxx" in uriTemplate)
    }

    @Test fun `should throw exception on illegal explode modifier`() {
        assertFailsWith<URITemplateException> { URITemplate.parse("{var*a}") }.let {
            expect("Explode indicator not followed by ',' or '}' at offset 5") { it.message }
        }
    }

    @Test fun `should handle Unicode surrogate pairs correctly`() {
        shouldConvert("{x:2}" to "~\uD83D\uDCA9".encodeUTF8().encodeSimple()) { this["x"] = "~\uD83D\uDCA9~" }
    }

    @Test fun `should perform substitutions listed in specification Overview`() {
        shouldConvert("http://example.com/~{username}/" to "http://example.com/~fred/") { this["username"] = "fred" }
        shouldConvert("http://example.com/~{username}/" to "http://example.com/~mark/") { this["username"] = "mark" }

        shouldConvert("http://example.com/dictionary/{term:1}/{term}" to "http://example.com/dictionary/c/cat") {
            this["term"] = "cat"
        }
        shouldConvert("http://example.com/dictionary/{term:1}/{term}" to "http://example.com/dictionary/d/dog") {
            this["term"] = "dog"
        }

        shouldConvert("http://example.com/search{?q,lang}" to "http://example.com/search?q=cat&lang=en") {
            this["q"] = "cat"
            this["lang"] = "en"
        }
        shouldConvert("http://example.com/search{?q,lang}" to "http://example.com/search?q=chien&lang=fr") {
            this["q"] = "chien"
            this["lang"] = "fr"
        }

        shouldConvert("http://www.example.com/foo{?query,number}" to
                "http://www.example.com/foo?query=mycelium&number=100") {
            this["query"] = "mycelium"
            this["number"] = 100
        }
        shouldConvert("http://www.example.com/foo{?query,number}" to "http://www.example.com/foo?number=100") {
            this["number"] = 100
        }
        shouldConvert("http://www.example.com/foo{?query,number}" to "http://www.example.com/foo")
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 1`() {
        shouldConvert("{var}" to "value") { setParameterValues() }
        shouldConvert("{hello}" to "Hello%20World%21") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 2`() {
        shouldConvert("{+var}" to "value") { setParameterValues() }
        shouldConvert("{+hello}" to "Hello%20World!") { setParameterValues() }
        shouldConvert("{+path}/here" to "/foo/bar/here") { setParameterValues() }
        shouldConvert("here?ref={+path}" to "here?ref=/foo/bar") { setParameterValues() }

        shouldConvert("X{#var}" to "X#value") { setParameterValues() }
        shouldConvert("X{#hello}" to "X#Hello%20World!") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 3`() {
        shouldConvert("map?{x,y}" to "map?1024,768") { setParameterValues() }
        shouldConvert("{x,hello,y}" to "1024,Hello%20World%21,768") { setParameterValues() }

        shouldConvert("{+x,hello,y}" to "1024,Hello%20World!,768") { setParameterValues() }
        shouldConvert("{+path,x}/here" to "/foo/bar,1024/here") { setParameterValues() }

        shouldConvert("{#x,hello,y}" to "#1024,Hello%20World!,768") { setParameterValues() }
        shouldConvert("{#path,x}/here" to "#/foo/bar,1024/here") { setParameterValues() }

        shouldConvert("X{.var}" to "X.value") { setParameterValues() }
        shouldConvert("X{.x,y}" to "X.1024.768") { setParameterValues() }

        shouldConvert("{/var}" to "/value") { setParameterValues() }
        shouldConvert("{/var,x}/here" to "/value/1024/here") { setParameterValues() }

        shouldConvert("{;x,y}" to ";x=1024;y=768") { setParameterValues() }
        shouldConvert("{;x,y,empty}" to ";x=1024;y=768;empty") { setParameterValues() }

        shouldConvert("{?x,y}" to "?x=1024&y=768") { setParameterValues() }
        shouldConvert("{?x,y,empty}" to "?x=1024&y=768&empty=") { setParameterValues() }

        shouldConvert("?fixed=yes{&x}" to "?fixed=yes&x=1024") { setParameterValues() }
        shouldConvert("{&x,y,empty}" to "&x=1024&y=768&empty=") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification Levels and Expression Types for Level 4`() {
        shouldConvert("{var:3}" to "val") { setParameterValues() }
        shouldConvert("{var:30}" to "value") { setParameterValues() }
        shouldConvert("{list}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{list*}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{keys}" to "semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{keys*}" to "semi=%3B,dot=.,comma=%2C") { setParameterValues() }

        shouldConvert("{+path:6}/here" to "/foo/b/here") { setParameterValues() }
        shouldConvert("{+list}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{+list*}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{+keys}" to "semi,;,dot,.,comma,,") { setParameterValues() }
        shouldConvert("{+keys*}" to "semi=;,dot=.,comma=,") { setParameterValues() }

        shouldConvert("{#path:6}/here" to "#/foo/b/here") { setParameterValues() }
        shouldConvert("{#list}" to "#red,green,blue") { setParameterValues() }
        shouldConvert("{#list*}" to "#red,green,blue") { setParameterValues() }
        shouldConvert("{#keys}" to "#semi,;,dot,.,comma,,") { setParameterValues() }
        shouldConvert("{#keys*}" to "#semi=;,dot=.,comma=,") { setParameterValues() }

        shouldConvert("X{.var:3}" to "X.val") { setParameterValues() }
        shouldConvert("X{.list}" to "X.red,green,blue") { setParameterValues() }
        shouldConvert("X{.list*}" to "X.red.green.blue") { setParameterValues() }
        shouldConvert("X{.keys}" to "X.semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("X{.keys*}" to "X.semi=%3B.dot=..comma=%2C") { setParameterValues() }

        shouldConvert("{/var:1,var}" to "/v/value") { setParameterValues() }
        shouldConvert("{/list}" to "/red,green,blue") { setParameterValues() }
        shouldConvert("{/list*}" to "/red/green/blue") { setParameterValues() }
        shouldConvert("{/list*,path:4}" to "/red/green/blue/%2Ffoo") { setParameterValues() }
        shouldConvert("{/keys}" to "/semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{/keys*}" to "/semi=%3B/dot=./comma=%2C") { setParameterValues() }

        shouldConvert("{;hello:5}" to ";hello=Hello") { setParameterValues() }
        shouldConvert("{;list}" to ";list=red,green,blue") { setParameterValues() }
        shouldConvert("{;list*}" to ";list=red;list=green;list=blue") { setParameterValues() }
        shouldConvert("{;keys}" to ";keys=semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{;keys*}" to ";semi=%3B;dot=.;comma=%2C") { setParameterValues() }

        shouldConvert("{?var:3}" to "?var=val") { setParameterValues() }
        shouldConvert("{?list}" to "?list=red,green,blue") { setParameterValues() }
        shouldConvert("{?list*}" to "?list=red&list=green&list=blue") { setParameterValues() }
        shouldConvert("{?keys}" to "?keys=semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{?keys*}" to "?semi=%3B&dot=.&comma=%2C") { setParameterValues() }

        shouldConvert("{&var:3}" to "&var=val") { setParameterValues() }
        shouldConvert("{&list}" to "&list=red,green,blue") { setParameterValues() }
        shouldConvert("{&list*}" to "&list=red&list=green&list=blue") { setParameterValues() }
        shouldConvert("{&keys}" to "&keys=semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{&keys*}" to "&semi=%3B&dot=.&comma=%2C") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Prefix Values`() {
        shouldConvert("{var}" to "value") { this["var"] = "value" }
        shouldConvert("{var:20}" to "value") { this["var"] = "value" }
        shouldConvert("{var:3}" to "val") { this["var"] = "value" }
        shouldConvert("{semi}" to "%3B") { this["semi"] = ";" }
        shouldConvert("{semi:2}" to "%3B") { this["semi"] = ";" }
    }

    @Test fun `should perform substitutions listed in specification for Composite Values`() {
        shouldConvert("/mapper{?address*}" to "/mapper?city=Newport%20Beach&state=CA") {
            this["address"] = mapOf(
                "city" to "Newport Beach",
                "state" to "CA",
            )
        }
        shouldConvert("find{?year*}" to "find?year=1965&year=2000&year=2012") {
            this["year"] = listOf("1965", "2000", "2012")
        }
        shouldConvert("www{.dom*}" to "www.example.com") {
            this["dom"] = listOf("example", "com")
        }
    }

    @Test fun `should perform substitutions listed in specification for Variable Expansion`() {
        shouldConvert("{count}" to "one,two,three") { setParameterValues() }
        shouldConvert("{count*}" to "one,two,three") { setParameterValues() }
        shouldConvert("{/count}" to "/one,two,three") { setParameterValues() }
        shouldConvert("{/count*}" to "/one/two/three") { setParameterValues() }
        shouldConvert("{;count}" to ";count=one,two,three") { setParameterValues() }
        shouldConvert("{;count*}" to ";count=one;count=two;count=three") { setParameterValues() }
        shouldConvert("{?count}" to "?count=one,two,three") { setParameterValues() }
        shouldConvert("{?count*}" to "?count=one&count=two&count=three") { setParameterValues() }
        shouldConvert("{&count*}" to "&count=one&count=two&count=three") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Simple String Expansion`() {
        shouldConvert("{var}" to "value") { setParameterValues() }
        shouldConvert("{hello}" to "Hello%20World%21") { setParameterValues() }
        shouldConvert("{half}" to "50%25") { setParameterValues() }
        shouldConvert("O{empty}X" to "OX") { setParameterValues() }
        shouldConvert("O{undef}X" to "OX") { setParameterValues() }
        shouldConvert("{x,y}" to "1024,768") { setParameterValues() }
        shouldConvert("{x,hello,y}" to "1024,Hello%20World%21,768") { setParameterValues() }
        shouldConvert("?{x,empty}" to "?1024,") { setParameterValues() }
        shouldConvert("?{x,undef}" to "?1024") { setParameterValues() }
        shouldConvert("?{undef,y}" to "?768") { setParameterValues() }
        shouldConvert("{var:3}" to "val") { setParameterValues() }
        shouldConvert("{var:30}" to "value") { setParameterValues() }
        shouldConvert("{list}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{list*}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{keys}" to "semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{keys*}" to "semi=%3B,dot=.,comma=%2C") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Reserved Expansion`() {
        shouldConvert("{+var}" to "value") { setParameterValues() }
        shouldConvert("{+hello}" to "Hello%20World!") { setParameterValues() }
        shouldConvert("{+half}" to "50%25") { setParameterValues() }

        shouldConvert("{base}index" to "http%3A%2F%2Fexample.com%2Fhome%2Findex") { setParameterValues() }
        shouldConvert("{+base}index" to "http://example.com/home/index") { setParameterValues() }
        shouldConvert("O{+empty}X" to "OX") { setParameterValues() }
        shouldConvert("O{+undef}X" to "OX") { setParameterValues() }

        shouldConvert("{+path}/here" to "/foo/bar/here") { setParameterValues() }
        shouldConvert("here?ref={+path}" to "here?ref=/foo/bar") { setParameterValues() }
        shouldConvert("up{+path}{var}/here" to "up/foo/barvalue/here") { setParameterValues() }
        shouldConvert("{+x,hello,y}" to "1024,Hello%20World!,768") { setParameterValues() }
        shouldConvert("{+path,x}/here" to "/foo/bar,1024/here") { setParameterValues() }

        shouldConvert("{+path:6}/here" to "/foo/b/here") { setParameterValues() }
        shouldConvert("{+list}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{+list*}" to "red,green,blue") { setParameterValues() }
        shouldConvert("{+keys}" to "semi,;,dot,.,comma,,") { setParameterValues() }
        shouldConvert("{+keys*}" to "semi=;,dot=.,comma=,") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Fragment Expansion`() {
        shouldConvert("{#var}" to "#value") { setParameterValues() }
        shouldConvert("{#hello}" to "#Hello%20World!") { setParameterValues() }
        shouldConvert("{#half}" to "#50%25") { setParameterValues() }
        shouldConvert("foo{#empty}" to "foo#") { setParameterValues() }
        shouldConvert("foo{#undef}" to "foo") { setParameterValues() }
        shouldConvert("{#x,hello,y}" to "#1024,Hello%20World!,768") { setParameterValues() }
        shouldConvert("{#path,x}/here" to "#/foo/bar,1024/here") { setParameterValues() }
        shouldConvert("{#path:6}/here" to "#/foo/b/here") { setParameterValues() }
        shouldConvert("{#list}" to "#red,green,blue") { setParameterValues() }
        shouldConvert("{#list*}" to "#red,green,blue") { setParameterValues() }
        shouldConvert("{#keys}" to "#semi,;,dot,.,comma,,") { setParameterValues() }
        shouldConvert("{#keys*}" to "#semi=;,dot=.,comma=,") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Label Expansion with Dot-Prefix`() {
        shouldConvert("{.who}" to ".fred") { setParameterValues() }
        shouldConvert("{.who,who}" to ".fred.fred") { setParameterValues() }
        shouldConvert("{.half,who}" to ".50%25.fred") { setParameterValues() }
        shouldConvert("www{.dom*}" to "www.example.com") { setParameterValues() }
        shouldConvert("X{.var}" to "X.value") { setParameterValues() }
        shouldConvert("X{.empty}" to "X.") { setParameterValues() }
        shouldConvert("X{.undef}" to "X") { setParameterValues() }
        shouldConvert("X{.var:3}" to "X.val") { setParameterValues() }
        shouldConvert("X{.list}" to "X.red,green,blue") { setParameterValues() }
        shouldConvert("X{.list*}" to "X.red.green.blue") { setParameterValues() }
        shouldConvert("X{.keys}" to "X.semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("X{.keys*}" to "X.semi=%3B.dot=..comma=%2C") { setParameterValues() }
        shouldConvert("X{.empty_keys}" to "X") { setParameterValues() }
        shouldConvert("X{.empty_keys*}" to "X") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Path Segment Expansion`() {
        shouldConvert("{/who}" to "/fred") { setParameterValues() }
        shouldConvert("{/who,who}" to "/fred/fred") { setParameterValues() }
        shouldConvert("{/half,who}" to "/50%25/fred") { setParameterValues() }
        shouldConvert("{/var}" to "/value") { setParameterValues() }
        shouldConvert("{/var,empty}" to "/value/") { setParameterValues() }
        shouldConvert("{/var,undef}" to "/value") { setParameterValues() }
        shouldConvert("{/var,x}/here" to "/value/1024/here") { setParameterValues() }
        shouldConvert("{/var:1,var}" to "/v/value") { setParameterValues() }
        shouldConvert("{/list}" to "/red,green,blue") { setParameterValues() }
        shouldConvert("{/list*}" to "/red/green/blue") { setParameterValues() }
        shouldConvert("{/list*,path:4}" to "/red/green/blue/%2Ffoo") { setParameterValues() }
        shouldConvert("{/keys}" to "/semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{/keys*}" to "/semi=%3B/dot=./comma=%2C") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Path-Style Parameter Expansion`() {
        shouldConvert("{;who}" to ";who=fred") { setParameterValues() }
        shouldConvert("{;half}" to ";half=50%25") { setParameterValues() }
        shouldConvert("{;empty}" to ";empty") { setParameterValues() }
        shouldConvert("{;v,empty,who}" to ";v=6;empty;who=fred") { setParameterValues() }
        shouldConvert("{;v,bar,who}" to ";v=6;who=fred") { setParameterValues() }
        shouldConvert("{;x,y}" to ";x=1024;y=768") { setParameterValues() }
        shouldConvert("{;x,y,empty}" to ";x=1024;y=768;empty") { setParameterValues() }
        shouldConvert("{;x,y,undef}" to ";x=1024;y=768") { setParameterValues() }
        shouldConvert("{;hello:5}" to ";hello=Hello") { setParameterValues() }
        shouldConvert("{;list}" to ";list=red,green,blue") { setParameterValues() }
        shouldConvert("{;list*}" to ";list=red;list=green;list=blue") { setParameterValues() }
        shouldConvert("{;keys}" to ";keys=semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{;keys*}" to ";semi=%3B;dot=.;comma=%2C") { setParameterValues() }
        shouldConvert("XXXX" to "XXXX") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Form-Style Query Expansion`() {
        shouldConvert("{?who}" to "?who=fred") { setParameterValues() }
        shouldConvert("{?half}" to "?half=50%25") { setParameterValues() }
        shouldConvert("{?x,y}" to "?x=1024&y=768") { setParameterValues() }
        shouldConvert("{?x,y,empty}" to "?x=1024&y=768&empty=") { setParameterValues() }
        shouldConvert("{?x,y,undef}" to "?x=1024&y=768") { setParameterValues() }
        shouldConvert("{?var:3}" to "?var=val") { setParameterValues() }
        shouldConvert("{?list}" to "?list=red,green,blue") { setParameterValues() }
        shouldConvert("{?list*}" to "?list=red&list=green&list=blue") { setParameterValues() }
        shouldConvert("{?keys}" to "?keys=semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{?keys*}" to "?semi=%3B&dot=.&comma=%2C") { setParameterValues() }
    }

    @Test fun `should perform substitutions listed in specification for Form-Style Query Continuation`() {
        shouldConvert("{&who}" to "&who=fred") { setParameterValues() }
        shouldConvert("{&half}" to "&half=50%25") { setParameterValues() }
        shouldConvert("?fixed=yes{&x}" to "?fixed=yes&x=1024") { setParameterValues() }
        shouldConvert("{&x,y,empty}" to "&x=1024&y=768&empty=") { setParameterValues() }
        shouldConvert("{&x,y,undef}" to "&x=1024&y=768") { setParameterValues() }

        shouldConvert("{&var:3}" to "&var=val") { setParameterValues() }
        shouldConvert("{&list}" to "&list=red,green,blue") { setParameterValues() }
        shouldConvert("{&list*}" to "&list=red&list=green&list=blue") { setParameterValues() }
        shouldConvert("{&keys}" to "&keys=semi,%3B,dot,.,comma,%2C") { setParameterValues() }
        shouldConvert("{&keys*}" to "&semi=%3B&dot=.&comma=%2C") { setParameterValues() }
    }

    private fun shouldConvert(pair: Pair<String, String>, block: URITemplate.() -> Unit = {}): URITemplate {
        val uriTemplate = URITemplate.parse(pair.first)
        uriTemplate.block()
        expect(pair.second) { uriTemplate.toString() }
        return uriTemplate
    }

    private fun URITemplate.setParameterValues() {
        for (parameterValue in parameterValues)
            if (contains(parameterValue.first))
                this[parameterValue.first] = parameterValue.second
    }

    private val parameterValues = listOf(
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
