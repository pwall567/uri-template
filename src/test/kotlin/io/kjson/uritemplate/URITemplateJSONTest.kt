/*
 * @(#) URITemplateJSONTest.kt
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
import kotlin.test.expect
import io.kjson.JSONArray

import io.kjson.JSONObject
import io.kjson.JSONString

class URITemplateJSONTest {

    @Test fun `should take JSONObject as variable resolver`() {
        val json = JSONObject.build {
            add("aaa", "something")
            add("bbb", 1234)
        }
        val template = URITemplate("/{aaa}/{bbb}")
        expect("/something/1234") { template.expand(json) }
    }

    @Test fun `should expand JSONArray like a List`() {
        val json = JSONObject.build {
            add("aaa", JSONArray.build {
                add("abc")
                add("def")
                add("ghi")
            })
        }
        val template = URITemplate("{/aaa*}")
        expect("/abc/def/ghi") { template.expand(json) }
    }

    @Test fun `should expand JSONObject Property like a List`() {
        val json = JSONObject.Property("abc", JSONString("def"))
        val template = URITemplate("{/aaa*}")
        expect("/abc/def") { template.expand { if (it == "aaa") json else null } }
    }

}
