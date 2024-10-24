# uri-template

[![Build Status](https://github.com/pwall567/uri-template/actions/workflows/build.yml/badge.svg)](https://github.com/pwall567/uri-template/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.9.24&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v1.9.24)
[![Maven Central](https://img.shields.io/maven-central/v/io.kjson/uri-template?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.kjson%22%20AND%20a:%uri-template%22)

Kotlin implementation of [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html).

## Background

The [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html) standard has been adopted by a number of frameworks for
the specification of URIs containing parameterized segments.

In addition, the [OpenAPI Specification](https://swagger.io/specification/) uses a format compatible with
[Level 1](#level-1) URI Templates for the specification of URIs in the `paths` object, and the
[JSON Schema Specification](https://json-schema.org/specification) specifies
[`uri-template`](https://json-schema.org/draft/2020-12/json-schema-validation#name-uri-template) as a format type for
[`format`](https://json-schema.org/draft/2020-12/json-schema-validation#name-vocabularies-for-semantic-c) validation.

 This means that the format is here to stay, creating the need for a library implementing it.

## Quick Start

In its simplest form (referred to in the specification as [Level 1](#level-1)), a template consists of a URI string with
variable sections denoted by the use of curly braces surrounding a variable name.

For example, the URI to retrieve a customer's details might be something like:
```
    http://example.com/customer/{customerId}
```

To employ this type of URI Template using the `uri-template` library:
```kotlin
    val template = URITemplate("http://example.com/customer/{customerId}")
    val uri = template.expand(mapOf("customerId" to customerId))
    callClient(uri)
```

The `expand()` function will return the expanded template as a `String` with the variable values supplied in a `Map` or
a `VariableResolver` substituted for the variable expressions.
If necessary, the substituted values are percent-encoded as required by
[Section 2.1](https://www.rfc-editor.org/rfc/rfc3986#section-2.1) of the
[URI Specification](https://www.rfc-editor.org/rfc/rfc3986).
If the mapping for a variable is not supplied, or if the variable is mapped to `null`, the substitution string will be
empty.

When the expanded form of the template is part of a larger string, the `expandTo()` function may be used to build the
string incrementally by appending to an `Appendable` (for example, a `StringBuilder`), avoiding the need to create
separate strings for each component.

The `URITemplate` object is immutable, and the same object may be used repeatedly with different variable mappings.

## Reference

### `URITemplate`

The `URITemplate` class has a single constructor, taking a `String` containing the template.
The string is parsed at construct time, throwing a [`URITemplateException`](#uritemplateexception) if any errors are
found.

To expand the template to a `String`, the following functions are available:
- `expand(resolver: VariableResolver)`: use the supplied [`VariableResolver`](#variableresolver) to resolve variable
  names.
- `expand(values: Map<String, Any?>)`: resolve variable names by lookup in the supplied `Map`.
- `expand(value: Pair<String, Any?>)`: resolve a single variable name using the supplied `Pair`.
- `expand()`: expand the template with no variable resolution.

Or to expand the template to an `Appendable` (_e.g._ a `StringBuilder`):
- `expandTo(a: Appendable, resolver: VariableResolver)`: use the supplied [`VariableResolver`](#variableresolver).

This last form is useful in cases where the expanded output is to be part of a larger `String`; it avoids unnecessary
memory allocation for partial strings.

### `VariableResolver`

`VariableResolver` is a functional interface; an implementation of this interface may be supplied to the `expand`
function to map variable names in the template to values to be substituted.
It has a single (operator) function `get` which takes a `String` and returns `Any?` but it will most often be
implemented using a lambda:
```kotlin
        val expanded = template.expand { if (it == "customerId") customerId else null }
```

This form of variable resolution, or its equivalent using `when`, can be particularly useful (as opposed to supplying
values in a `Map`) when the values are to be lazily acquired.

### `URITemplateException`

Any errors in the parsing of the template will result in a `URITemplateException`.
The `message` property of the exception object will contain a useful explanatory message, but in addition, a
`TextMatcher` object (see the [`textmatcher`](https://github.com/pwall567/textmatcher) project) will provide details of
location of the error in the input string, where relevant.

The `URITemplateException` exposes the following properties:

| Name      | Type           | Notes                                                                         |
|-----------|----------------|-------------------------------------------------------------------------------|
| `message` | `String`       | The full message, including the offset in the template string, where relevant |
| `text`    | `String`       | The text of the message without location information                          |
| `tm`      | `TextMatcher?` | The `TextMatcher` object, where relevant                                      |

The `TextMatcher` will not be supplied in cases such an unclosed variable expression &ndash; this will only be detected
at the end of the string.

## Variables

The specification describes how various data types are to be handled when used as values to be substituted into
variables, but it is written in general terms unrelated to any particular computer language.

When the specification describes the handling of lists, this library applies those rules to arrays and to any objects
implementing the `Iterable` interface (including `List`, `Set` _etc._).
For convenience, it will also treat a `Pair` or a `Triple` as a form of list, and it will also treat a `Map.Entry` as a
list &ndash; this is a consequence of the prescribed handling of &ldquo;associative arrays&rdquo; in the absence of the
&ldquo;Explode&rdquo; modifier, as set out in [Section 3.2.1](https://www.rfc-editor.org/rfc/rfc6570.html#section-3.2.1)
of the specification.

What the specification refers to as &ldquo;associative arrays&rdquo; are values of type `Map` in this implementation,
and therefore the `Map.Entry` represents the (name, value) pair described in the above-mentioned section. 

All other non-null values are output as strings using the `toString()` function of the value, and null or missing
values result in an empty string.

## Level 1

The [URI Template](https://www.rfc-editor.org/rfc/rfc6570.html) specification describes the simple substitution of a
string, without modification and using standard percent encoding, as &ldquo;Level 1&rdquo; templates.
This form of template is the form most commonly used by other systems, and if compatibility with a broad range of
systems is a priority, 

All versions of the `uri-template` library support this level.

## Level 2

The specification describes a &ldquo;Level 2&rdquo;, adding operator characters which modify the expansion of the
variable expressions.
See [Section 1.2](https://www.rfc-editor.org/rfc/rfc6570.html#section-1.2) of the specification for more details.

Version 2.0 (and later) of the `uri-template` library supports this level.

## Level 3

&ldquo;Level 3&rdquo; of the specification adds further modifying operators, as well as allowing multiple variables in
expressions.

Version 3.0 (and later) of the `uri-template` library supports this level.

## Level 4

&ldquo;Level 4&rdquo; of the specification adds the concept of a value modifier as a suffix on each variable;
the &ldquo;Prefix&rdquo; modifier limits the expansion of string values to a prefix of a specified length, and the
&ldquo;Explode&rdquo; modifier changes the handling of lists and &ldquo;associative arrays&rdquo; (see
[Variables](#variables) above) in a manner similar to the &ldquo;spread&rdquo; operators in some languages.
See [Section 2.4](https://www.rfc-editor.org/rfc/rfc6570.html#section-2.4) of the specification for more details.

Version 4.0 (and later) of the `uri-template` library supports this level.

## Examples

The specification has a large number of examples throughout the document, and these examples are a valuable resource for
anyone attempting to understand the specification.

The main unit test class for this library tests every example in the document, confirming that the library behaves as
specified in all cases.

## Dependency Specification

The latest version of the library is 4.0, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>io.kjson</groupId>
      <artifactId>uri-template</artifactId>
      <version>4.0</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'io.kjson:uri-template:4.0'
```
### Gradle (kts)
```kotlin
    implementation("io.kjson:uri-template:4.0")
```

Peter Wall

2024-10-25
