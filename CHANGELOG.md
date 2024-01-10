# Changelog

## [Unreleased]

## [5.1.27]
### Changed
- Guava version update to address a vulnerability on `33.0.0-android`, by @HardNorth
- Okhttp version update to address a vulnerability on `4.12.0`, by @HardNorth
- Logback version update to address a vulnerability on `1.3.12`, by @HardNorth

## [5.1.26]
### Added
- `MarkdownUtils.formatDataTable` and `ParameterUtils.formatParametersAsTable` methods to support parameter reporting for BDD frameworks, by @HardNorth

## [5.1.25]
### Changed
- Static methods of `AttributeParser` are now public, Javadoc and JSR annotations added, by @HardNorth

## [5.1.24]
### Added
- `ReportPortalClient.updateLaunch` method, by @matt-richardson
### Changed
- `SecondaryLaunchFinishCondition` class was separated to resolve fat jar issues, by @HardNorth
- `rxjava` was forcibly excluded from `retrofit2:adapter-rxjava2` transitive dependencies to resolve fat jar issues, by @HardNorth

## [5.1.23]
### Changed
- Unified ReportPortal product naming, by @HardNorth

## [5.1.22]
### Changed
- Async reporting is now used by default, by @HardNorth

## [5.1.21]
### Added
- `rp.launch.uuid.print` and `rp.launch.uuid.print.output` configuration parameters, by @HardNorth
### Changed
- Slf4j version updated on version 2.0.4 to support newer versions of Logback with security fixes, by @HardNorth

## [5.1.20]
### Changed
- AspectJ version updated on 1.9.19 to support newer versions of Java and bug fixes, by @HardNorth
- Some classes were refactored to less use Guava library and more core Java, by @HardNorth
### Added
- `ClientIdProvider` class to store and read a client's unique ID, by @HardNorth
- `StepReporter.setStepStatus` method to change nested step status in runtime, by @HardNorth

## [5.1.18]
### Added
- `rp.http.proxy.username` and `rp.http.proxy.password` parameters, by @HardNorth
### Fixed
- `NullPointerException` in `TestCaseIdUtils` class for case with null-value parameters, by @HardNorth

## [5.1.17]
### Added
- `class` and `classRef` keywords for `@TestCaseId` templating, by @HardNorth

## [5.1.16]
### Added
- `PropertiesLoader.overrideWith(java.util.Properties)` method, by @HardNorth
### Changed
- `MemoizingSupplier` class was refactored to get rid of `synchronized` keyword, by @HardNorth
- `LogBatchingFlowable` class was refactored to get rid of `synchronized` keyword, by @HardNorth
- `LaunchIdLockSocket` class was refactored to get rid of `synchronized` keyword, by @HardNorth
- `StatisticsService` was rewritten on new protocol version, by @HardNorth

## [5.1.15]
### Added
- `class` and `classRef` keywords for `@Step` templating, by @HardNorth
- `PropertiesLoader.getPropertyFilePath`method, by @HardNorth

## [5.1.14]
### Added
- Issue [#198](https://github.com/reportportal/client-java/issues/198) Property file customization with `rp.properties.path` property, by @HardNorth
### Changed
- `jackson-databind` dependency was forcibly updated to address vulnerabilities, by @HardNorth

## [5.1.12]
### Fixed
- Launch finish time in case of fork-join launch, by @PavelSakharchuk
### Added
- `rp.launch.uuid` property, to append to and existing launch without start/finish launch calls, by @HardNorth
- Attribute values truncation and its properties, by @HardNorth
- Some javadocs, by @HardNorth 
### Changed
- `Launch` objects do not modify request objects which were passed to them now, cloning them instead, by @HardNorth
- Some refactoring, by @HardNorth

## [5.1.11]
### Added
- `StepReporter#step(ItemStatus, String, Supplier<T>)` method, by @HardNorth
- Log batching payload size tracking, by @HardNorth
- `rp.batch.payload.limit` configuration parameter, by @HardNorth
### Removed
- `finally` keyword, see [JEP 421](https://openjdk.java.net/jeps/421), by @HardNorth
### Changed
- Logging RxJava flow refactoring, by @HardNorth

## [5.1.10]
### Added
- public `StepNameUtils.getStepName` methods to ease template processing customizations, by @HardNorth

## [5.1.9]
### Changed
- `jackson-databind` dependency was forcibly updated to address vulnerabilities, by @HardNorth 

## [5.1.8]
### Added
- Issue [#70](https://github.com/reportportal/client-java/issues/70) `@Step` templating now supports `this` object reference
- Issue [#130](https://github.com/reportportal/client-java/issues/130) `@TestCaseId` now supports templating

## [5.1.7]
### Added
- `@Attributes` annotation inheritance
- `Launch.getLaunch` method, which returns current Launch UUID or empty `Maybe`
- `LoggingContext.context()` public static method which allows get current logging context
### Fixed
- Issue [#182](https://github.com/reportportal/client-java/issues/182): Duplicate key error
- Issue [#147](https://github.com/reportportal/client-java/issues/147): Logging context loose in child thread
- Null-pointer cases in LoggingContext class

## [5.1.4]
### Changed
- Slf4j version updated on 1.7.32 to support newer versions of Logback with security fixes

## [5.1.3]
### Changed
- JVM arg parameters have more priority than Environment variables now.
- Property loader doesn't throw any errors now, just logs warnings.

## [5.1.2]
### Added
- HTTP timeout parameters

## [5.1.1]
### Fixed
- TestNG NestedStep mixing issue

## [5.1.0]
### Added
- Method `StepReporter.finishNestedStep(ItemStatus)`
### Changed
- Exception logging on lambda-style nested step was removed to avoid double-logging, since it should be logged on test level.

## [5.1.0-RC-12]
### Changed
- Nested Step finish refactoring
### Added
- More JSR-305 annotations

## [5.1.0-RC-11]
### Changed
- StepRequestUtils refactoring
### Added
- `StepReporter#finishPreviousStep(@Nullable ItemStatus status);` method

## [5.1.0-RC-10]
### Added
- `StepReporter#step` methods, issue [#127](https://github.com/reportportal/client-java/issues/127) 

## [5.1.0-RC-9]
### Changed
- Refactoring of StepAspect class
- StepRequestUtils class now public

## [5.1.0-RC-7]
### Added
- Error message on incompatible OkHttp dependency version
- It's now possible to obtain ReportPortal client by `Launch.currentLaunch().getClient()`

## [5.1.0-RC-6]

## [5.1.0-RC-5]
### Fixed
- Lock wait timeout property initialization

## [5.1.0-RC-2]
### Fixed
- Incorrect MIME type attachment upload

## [5.1.0-RC-1]
### Added
- New distributed Launch join mechanism: sockets
- New properties: `rp.client.join.mode`, `rp.client.join.file.lock.name`, `rp.client.join.file.sync.name`, `rp.client.join.timeout.value`, 
  `rp.client.join.timeout.unit`, `rp.client.join.lock.timeout.value`, `rp.client.join.lock.timeout.unit`, `rp.client.join.port`
### Changed
- UniqueID annotation deprecated
- It's now possible to assign Attributes annotation on classes
- Property deprecation: `rp.client.join.lock.file.name`, `rp.client.join.sync.file.name`, `rp.client.join.file.wait.timeout.ms`
### Removed
- Apache Tika dependency
- Tags annotation since it looks like it is not in use anywhere

## [5.1.0-BETA-1]
### Changed
- Apache Tika version degraded on 1.19 for Android compatibility
- Common Jackson ObjectMapper moved to HttpRequestUtils class
- try-catch refactoring in `ReportPortal.Builder` class to get better stack traces
- Introducing "Retrofit 2" as RP client

## [5.0.22]
### Added
- Item names truncation, issue #154

## [5.0.21]
### Changed
- try-catch refactoring in `ReportPortal.Builder` class to get better stack traces
### Added
- `rp.rx.buffer.size` property handling

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
  com.epam.reportportal.service.ReportPortal.emitLog(io.reactivex.Maybe<java.lang.String>, 
java.util.function.Function<java.lang.String,com.epam.ta.reportportal.ws.model.log.SaveLogRQ>)
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
