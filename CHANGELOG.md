# Changelog

## [Unreleased]

## [5.1.0-ALPHA-6]
### Fixed
- Bug fixes
### Changed
- Apache Tika version degraded on 1.19 for Android compatibility
- Common Jackson ObjectMapper moved to HttpRequestUtils class
- try-catch refactoring in `ReportPortal.Builder` class to get better stack traces
- Introducing "Retrofit 2" as RP client

## [5.0.18]
### Added
- A static Issue Launch.NOT_ISSUE to use in agents and avoid issue creation duplication
- StatusEvaluation class with an `evaluateStatus` method

## [5.0.17]
### Fixed
- All client threads now marked as daemon to not prevent JVM shutting down

## [5.0.16]
### Added
- 'AGENT_NO_ANALYTICS' environment variable
- MemoizingSupplier class for agents lazy init

## [5.0.15]
### Added
- A method to emit a log entry for a certain item: 
  com.epam.reportportal.service.ReportPortal.emitLog(io.reactivex.Maybe<java.lang.String>, java.util.function.Function<java.lang.String,com.epam.ta.reportportal.ws.model.log.SaveLogRQ>)
### Changed
- Apache Tika was updated on version 1.20

## [5.0.14]
### Changed
- Test Case ID generation methods now accepts `Executable` as a parameter instead of `Method`

## [5.0.13]
### Fixed
- A lot of code style warnings (e.g. legacy java 6 code updated, etc)
### Added
- TestItemTree.TestItemLeaf now supports attribute storing
### Changed
- Properties loader now ignores property case

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
