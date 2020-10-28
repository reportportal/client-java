# Changelog

## [Unreleased]
### Fixed
- A lot of code style warnings (e.g. legacy java 6 code updated, etc)
### Added
- TestItemTree.TestItemLeaf now supports attribute storing

## [5.0.12]
### Fixed
- Null-value handling for inner fields for step templates of annotation-based nested steps
### Changed
- It's now possible to bypass a null-value client to ReportPortal class constructor, as a result user will get a NOOP launch

## [5.0.11]
### Changed
- ReportPortal does not throw any exceptions if 'reportportal.properties' not found

## [5.0.10]
### Added
- Additional ParameterUtils methods to use inside Cucumber agents
### Changed
- Manual Nested Step failure now also fails all the ancestors, not only the nearest one

## [5.0.7]
### Added
- StepAspect.setParentId and StepAspect.removeParentId now handled inside LaunchImpl class 

## [5.0.6]
### Added
- Utilities
  - More Test Case ID methods
  - CodeRef generation method
- A proxy parameter handling: `rp.http.proxy=http://localhost:8981`
### Fixed
- Issue #117 multithreaded reporting of nested steps 

## [5.0.2]
### Added
- AnalyticsService class to publish Google Analytics events
- LaunchImpl now publish Analytics Events
- ParameterUtils class which will be responsible for ParameterResource lists generation based on methods/constructors
### Changed
- @NotNull annotations replaced with @Nonnull as the latter is shorter and comes with Java SE

## [4.0.4]
### Improvements
* reportportal/reportportal#293 - Re-licence client side 

## [4.0.0]
### Improvements
* Get rid of HTTP Apache Async Client in favor of synchronous version 
* Better extensibility

## [3.0.3]
### Released: 9 June 2017
### Bugfixes
* reportportal/reportportal#142 - Java client parameters are not UTF-8 encoded
