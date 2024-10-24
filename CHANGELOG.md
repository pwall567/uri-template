# Change Log

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## [4.0] - 2024-10-25
### Added
- `VariableReference`: to allow modifiers to be added to each use of a variable individually
- `BooleanOps`: a simple "test and set" functionality
### Changed
- `URITemplate`: changed to make constructor public, to make object immutable and make `parse()` function private
- `URITemplate`, `ExpressionElement`: major re-work, switched to use `VariableReference`
- `ExpressionElement`: implemented Level 4 functionality
- `ExpressionElement`: fixed bug in Unicode surrogate pair handling
- `URITemplate`: improved error handling
- `URITemplate`: changed to use `expand()` instead of `toString()`, and added `expandTo()`

## [3.0] - 2024-10-15
### Added
- `ExpressionElement`: reinstated in expanded form
### Removed
- `VariableElement`, `ReservedElement`, `FragmentElement`, `DotPrefixedElement`, `SlashPrefixedElement`,
  `SemicolonPrefixedElement`, `QueryElement`, `QueryContinuationElement`
### Changed
- `URITemplate`: added support for specification level 3
- `URITemplate`: modified to make better use of `TextMatcher` parsing
- `URITemplate`: reverted to use of parameterized `ExpressionElement`

## [2.0] - 2024-10-14
### Added
- `VariableElement`, `ReservedElement`, `FragmentElement`
### Removed
- `ExpressionElement`
### Changed
- `URITemplate`, `Element`: added support for specification level 2

## [1.0] - 2024-10-14
### Added
- multiple files: library complete to specification level 1

## [0.1] - 2024-10-12
### Added
- all files: initial version (work in progress)
