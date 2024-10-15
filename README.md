# uri-template

[![Build Status](https://github.com/pwall567/uri-template/actions/workflows/build.yml/badge.svg)](https://github.com/pwall567/uri-template/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.9.24&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.9.24)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/uri-template?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%uri-template%22)

Kotlin implementation of [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html).

## Background

The [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html) standard has been adopted by a number of frameworks for
the specification of URIs containing parameterized segments.

In addition, the [OpenAPI Specification](https://swagger.io/specification/) uses a format identical to
[Level 1](#level-1) URI Templates for the specification of URIs in the `paths` object, and the
[JSON Schema Specification](https://json-schema.org/specification) specifies
[`uri-template`](https://json-schema.org/draft/2020-12/json-schema-validation#name-uri-template) as a format type for
[`format`](https://json-schema.org/draft/2020-12/json-schema-validation#name-vocabularies-for-semantic-c) validation.

## Usage

In its simplest form (referred to in the specification as Level 1), a template consists of a URI string with variable
sections denoted by the use of curly braces surrounding a variable name.

For example, the URI to retrieve a customer's details might be something like:
```
    http://example.com/customer/{customerId}
```

To employ this type of URI Template using the `uri-template` library:
```kotlin
    val template = URITemplate.parse("http://example.com/customer/{customerId}")
    template["customerId"] = customerId
    callClient(template.toString())
```

The `toString()` function will return the expanded template as a `String` with the variable values substituted for the
variable expressions.
If necessary, the values are percent-encoded as required by
[Section 2.1](https://www.rfc-editor.org/rfc/rfc3986#section-2.1) of the
[URI Specification](https://www.rfc-editor.org/rfc/rfc3986).
If the value has not been set, or if it has been set to `null`, the substitution string will be empty.

The `URITemplate` object is not an implementation of the `Map` interface, but it implements the `get()` and `set()`
operations as if it were a `MutableMap<String, Any?>`, with the limitation that the key must have been specified as a
variable name in the template string.
So `"customerId"` is a valid key in the above example, but `"customer"` is not (and will cause an exception to be
thrown).

The `URITemplate` object is mutable, and therefore is not thread-safe, so the expected pattern of usage is to create the
object, set its variables, use it and then discard it.
For anyone concerned about the run-time cost of parsing the template string every time, the `copy()` function will
create a copy of a template (including current variable values).

To check whether a variable name has been specified in the template, the `contains()` function will allow the use of the
Kotlin `in` syntax.

And the function `clear()` will reset the values of all variables to `null`.

## Level 1

The [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html) specification describes the simple substitution of a
string as "Level 1" templates.

The current version of the `uri-template` library conforms to this level; refer to the specification for further details
of the substitutions performed.

## Level 2

The specification describes a "Level 2", adding operator characters which modify the expansion of the variables.
See [Section 1.2](https://www.rfc-editor.org/rfc/rfc6570.html#section-1.2) of the specification for more details.

Version 2.0 (and later) of the `uri-template` library supports this level.

## Level 3

"Level 3" of the specification adds further modifying operators, as well as allowing multiple variables in expressions.

Version 3.0 (and later) of the `uri-template` library supports this level.

## Level 4

"Level 4" of the specification adds the concept of a variable modifier as a suffix on each variable.  

The `uri-template` library does not currently support this level.

## Dependency Specification

The latest version of the library is 3.0, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>uri-template</artifactId>
      <version>3.0</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:uri-template:3.0'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:uri-template:3.0")
```

Peter Wall

2024-10-15
