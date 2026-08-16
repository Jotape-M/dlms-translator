# Changelog

All notable changes to the DLMS Translator plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-08-16

### Removed
- Portuguese (PT-BR) translation

## [1.2.0] - 2026-08-15

### Added
- "Translate DLMS Frame" editor context menu action
- Select any hex frame in an editor, right-click, and translate it instantly
- Automatic normalization of common log formats (TX:/RX: prefixes, 0x prefixes, various separators)
- Clear IDE notifications for invalid or empty selections

## [1.1.1] - 2026-04-04

### Added
- Scrolling support in the XML output area (vertical and horizontal scrollbars)

## [1.1.0] - 2026-03-15

### Added
- Base64 input support alongside hexadecimal input
- Input type selector dropdown (Hex/Base64) in the tool window UI
- Validation for Base64 input with appropriate error messages

### Changed
- Updated input placeholder text to reflect both Hex and Base64 support
- Improved error messages to be input-type agnostic
- Refined UI layout and component spacing

## [1.0.0] - Initial Release

### Added
- DLMS PDU translation from hexadecimal to XML
- Tool window with input/output interface
- Hexadecimal output toggle option
- XML formatting and syntax highlighting
- Support for DLMS/COSEM protocol frames (AARQ, AARE, get-request, etc.)
- Integration with Gurux DLMS Java library
