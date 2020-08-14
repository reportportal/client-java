# Changelog

## [Unreleased]
### Added
- More Test Case ID methods

## [5.0.3]
### Added
- Utilities
  - More Test Case ID methods
  - CodeRef generation method
- A proxy parameter handling: `rp.http.proxy=http://localhost:8981`

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
##### Released: 9 June 2017

### Bugfixes

* reportportal/reportportal#142 - Java client parameters are not UTF-8 encoded


