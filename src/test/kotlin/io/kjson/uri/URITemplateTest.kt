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

    @Test fun `should expand simple template`() {
        val uriTemplate = URITemplate.parse("http://kjson.io")
        expect("http://kjson.io") { uriTemplate.toString() }
    }

    @Test fun `should create empty template`() {
        val uriTemplate = URITemplate.parse("")
        assertTrue(uriTemplate.elements.isEmpty())
    }

    @Test fun `should expand empty template`() {
        val uriTemplate = URITemplate.parse("")
        expect("") { uriTemplate.toString() }
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
        val uriTemplate = URITemplate.parse("Text-%22%25%22")
        expect("Text-%22%25%22") { uriTemplate.toString() }
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
                    assertIs<SimpleElement>(this)
                    with(names) {
                        expect(1) { size }
                        expect("var") { this[0] }
                    }
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
        val uriTemplate = URITemplate.parse("(prefix){var}(suffix)")
        uriTemplate["var"] = "value"
        expect("(prefix)value(suffix)") { uriTemplate.toString() }
        expect("value") { uriTemplate["var"] }
    }

    @Test fun `should expand template with unset variable`() {
        val uriTemplate = URITemplate.parse("(prefix){var}(suffix)")
        expect("(prefix)(suffix)") { uriTemplate.toString() }
    }

    @Test fun `should expand template with percent encoding`() {
        val uriTemplate = URITemplate.parse("(prefix){var}(suffix)")
        uriTemplate["var"] = "< >"
        expect("(prefix)%3C%20%3E(suffix)") { uriTemplate.toString() }
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
                    assertIs<SimpleElement>(this)
                    with(names) {
                        expect(1) { size }
                        expect("var") { this[0] }
                    }
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
                    assertIs<ReservedElement>(this)
                    expect(listOf("var")) { names }
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
        val uriTemplate = URITemplate.parse("(prefix){+var}(suffix)")
        uriTemplate["var"] = "/abc/def"
        expect("(prefix)/abc/def(suffix)") { uriTemplate.toString() }
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
                    assertIs<FragmentElement>(this)
                    expect(listOf("var")) { names }
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
        val uriTemplate = URITemplate.parse("(prefix){#var}(suffix)")
        uriTemplate["var"] = "abc$"
        expect("(prefix)#abc$(suffix)") { uriTemplate.toString() }
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
                    assertIs<SimpleElement>(this)
                    with(names) {
                        expect(2) { size }
                        expect("var1") { this[0] }
                        expect("var2") { this[1] }
                    }
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
        val uriTemplate = URITemplate.parse("(prefix){var1,var2}(suffix)")
        uriTemplate["var1"] = "alpha"
        uriTemplate["var2"] = "beta"
        expect("(prefix)alpha,beta(suffix)") { uriTemplate.toString() }
        expect("alpha") { uriTemplate["var1"] }
        expect("beta") { uriTemplate["var2"] }
    }

    @Test fun `should perform substitutions listed in specification for Level 1`() {
        val uriTemplate1 = URITemplate.parse("{var}").apply {
            this["var"] = "value"
        }
        expect("value") { uriTemplate1.toString() }
        val uriTemplate2 = URITemplate.parse("{hello}").apply {
            this["hello"] = "Hello World!"
        }
        expect("Hello%20World%21") { uriTemplate2.toString() }
    }

    @Test fun `should perform substitutions listed in specification for Level 2`() {
        val uriTemplate1 = URITemplate.parse("{+var}").apply {
            this["var"] = "value"
        }
        expect("value") { uriTemplate1.toString() }
        val uriTemplate2 = URITemplate.parse("{+hello}").apply {
            this["hello"] = "Hello World!"
        }
        expect("Hello%20World!") { uriTemplate2.toString() }
        val uriTemplate3 = URITemplate.parse("{+path}/here").apply {
            this["path"] = "/foo/bar"
        }
        expect("/foo/bar/here") { uriTemplate3.toString() }
    }

    @Test fun `should perform substitutions listed in specification for Level 3`() {
        val uriTemplate1 = URITemplate.parse("map?{x,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
        }
        expect("map?1024,768") { uriTemplate1.toString() }
        val uriTemplate2 = URITemplate.parse("{x,hello,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
            this["hello"] = "Hello World!"
        }
        expect("1024,Hello%20World%21,768") { uriTemplate2.toString() }
        val uriTemplate3 = URITemplate.parse("{+x,hello,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
            this["hello"] = "Hello World!"
        }
        expect("1024,Hello%20World!,768") { uriTemplate3.toString() }
        val uriTemplate4 = URITemplate.parse("{+path,x}/here").apply {
            this["x"] = "1024"
            this["path"] = "/foo/bar"
        }
        expect("/foo/bar,1024/here") { uriTemplate4.toString() }
        val uriTemplate5 = URITemplate.parse("{#x,hello,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
            this["hello"] = "Hello World!"
        }
        expect("#1024,Hello%20World!,768") { uriTemplate5.toString() }
        val uriTemplate6 = URITemplate.parse("{#path,x}/here").apply {
            this["x"] = "1024"
            this["path"] = "/foo/bar"
        }
        expect("#/foo/bar,1024/here") { uriTemplate6.toString() }
        val uriTemplate7 = URITemplate.parse("X{.var}").apply {
            this["var"] = "value"
        }
        expect("X.value") { uriTemplate7.toString() }
        val uriTemplate8 = URITemplate.parse("X{.x,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
        }
        expect("X.1024.768") { uriTemplate8.toString() }
        val uriTemplate9 = URITemplate.parse("{/var}").apply {
            this["var"] = "value"
        }
        expect("/value") { uriTemplate9.toString() }
        val uriTemplate10 = URITemplate.parse("{/var,x}/here").apply {
            this["var"] = "value"
            this["x"] = "1024"
        }
        expect("/value/1024/here") { uriTemplate10.toString() }
        val uriTemplate11 = URITemplate.parse("{;x,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
        }
        expect(";x=1024;y=768") { uriTemplate11.toString() }
        val uriTemplate12 = URITemplate.parse("{;x,y,empty}").apply {
            this["x"] = "1024"
            this["y"] = "768"
            this["empty"] = ""
        }
        expect(";x=1024;y=768;empty") { uriTemplate12.toString() }
        val uriTemplate13 = URITemplate.parse("{?x,y}").apply {
            this["x"] = "1024"
            this["y"] = "768"
        }
        expect("?x=1024&y=768") { uriTemplate13.toString() }
        val uriTemplate14 = URITemplate.parse("{?x,y,empty}").apply {
            this["x"] = "1024"
            this["y"] = "768"
            this["empty"] = ""
        }
        expect("?x=1024&y=768&empty=") { uriTemplate14.toString() }
        val uriTemplate15 = URITemplate.parse("?fixed=yes{&x}").apply {
            this["x"] = "1024"
        }
        expect("?fixed=yes&x=1024") { uriTemplate15.toString() }
        val uriTemplate16 = URITemplate.parse("{&x,y,empty}").apply {
            this["x"] = "1024"
            this["y"] = "768"
            this["empty"] = ""
        }
        expect("&x=1024&y=768&empty=") { uriTemplate16.toString() }
    }

}
