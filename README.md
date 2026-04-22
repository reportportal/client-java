# Client java

> **DISCLAIMER**: We use Google Analytics for sending anonymous usage information such as agent's and client's names,
> and their versions after a successful launch start. This information might help us to improve both ReportPortal
> backend and client sides. It is used by the ReportPortal team only and is not supposed for sharing with 3rd parties.

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/client-java.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.epam.reportportal/client-java)
[![CI Build](https://github.com/reportportal/client-java/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/client-java/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/client-java/branch/develop/graph/badge.svg?token=IVTys0o4JT)](https://codecov.io/gh/reportportal/client-java)
[![Join Slack chat!](https://img.shields.io/badge/slack-join-brightgreen.svg)](https://slack.epmrpp.reportportal.io/)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The latest version: 5.4.13. Please use `Maven Central` link above to get the client.

## JVM-based clients configuration

### How to provide parameters

There are several ways to load parameters. Be aware that higher sources override lower ones. For example, properties
from file can be overridden by JVM variables.

| Order | Source                |
|-------|-----------------------|
| 1     | JVM arguments         |
| 2     | Environment variables |
| 3     | Properties file       |

### JVM arguments

ReportPortal client does not necessarily need properties file to be configured. One of the option is to use JVM
arguments which have the highest priority among configuration ways. To use them you need to specify them in command line
after Java executable using `-D` flag.
Example:

```shell
$ java -Drp.endpoint=https://rp.epam.com/ -jar my-tests.jar
```

### Environment variables

In case of bypassing parameters through environment variables they should be specified in UPPERCASE separated by
underscores (`_`).
E.G.:

* `rp.endpoint` --> `RP_ENDPOINT`
* `rp.skipped.issue` --> `RP_SKIPPED_ISSUE`

### Property file

The most common way to start using an agent is to copy your configuration from UI of ReportPortal at User Profile
section or configure property file `reportportal.properties` in the following format:

```properties
rp.endpoint=https://rp.epam.com/
rp.api.key=8967de3b-fec7-47bb-9dbc-2aa4ceab8b1e
rp.launch=launch-name
rp.project=project-name
## OPTIONAL PARAMETERS
rp.reporting.async=true
rp.reporting.callback=true
rp.enable=true
rp.description=My awesome launch
rp.attributes=key:value;value
rp.rerun=true
rp.rerun.of=ae586912-841c-48de-9391-50720c35dd5a
rp.convertimage=true
rp.mode=DEFAULT
rp.skipped.issue=true
rp.batch.size.logs=20
rp.keystore.resource=<PATH_TO_YOUR_KEYSTORE>
rp.keystore.password=<PASSWORD_OF_YOUR_KEYSTORE>
```

For detailed parameter description see below sections.

Default properties file should have `reportportal.properties` name. It can be situated in the class path (in the project
directory) and if client can’t find the file it logs a warning. But you can also use your custom property file 
specifying file's path in `rp.properties.path` system property or `RP_PROPERTIES_PATH` environment variable. The first 
option has priority, so if you specify the path in both system properties and environment variables then system property
value will be used.

## Parameters

### Common parameters

| **Property name**            | **Type**  | **Description**                                                                                                                                                                                                                                                                                                                                                              | **Required** |
|------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| rp.endpoint                  | String    | URL of web service, where requests should be send                                                                                                                                                                                                                                                                                                                            | Yes          |
| rp.api.key                   | String    | Api token of user. **Required** if OAuth 2.0 authentication is not used.                                                                                                                                                                                                                                                                                                     | Conditional  |
| rp.oauth.token.uri           | String    | OAuth 2.0 token endpoint URL for password grant authentication. **Required** if API key is not used.                                                                                                                                                                                                                                                                         | Conditional  |
| rp.oauth.username            | String    | OAuth 2.0 username for password grant authentication. **Required** if OAuth 2.0 is used.                                                                                                                                                                                                                                                                                     | Conditional  |
| rp.oauth.password            | String    | OAuth 2.0 password for password grant authentication. **Required** if OAuth 2.0 is used.                                                                                                                                                                                                                                                                                     | Conditional  |
| rp.oauth.client.id           | String    | OAuth 2.0 client identifier. **Required** if OAuth 2.0 is used.                                                                                                                                                                                                                                                                                                              | Conditional  |
| rp.oauth.client.secret       | String    | OAuth 2.0 client secret. **Optional** for OAuth 2.0 authentication.                                                                                                                                                                                                                                                                                                          | No           |
| rp.oauth.scope               | String    | OAuth 2.0 access token scope. **Optional** for OAuth 2.0 authentication.                                                                                                                                                                                                                                                                                                     | No           |
| rp.oauth.use.proxy           | Boolean   | Default: `true`<br /> Determines if OAuth HTTP client Inherits proxy settings from general client                                                                                                                                                                                                                                                                            | No           |
| rp.launch                    | String    | A unique name of Launch (Run). Based on that name a history of runs will be created for particular name                                                                                                                                                                                                                                                                      | Yes          |
| rp.project                   | String    | Project name to identify scope                                                                                                                                                                                                                                                                                                                                               | Yes          |
| rp.launch.uuid               | String    | A unique Launch UUID to which the whole test execution will be uploaded.                                                                                                                                                                                                                                                                                                     | No           |
| rp.launch.uuid.creation.skip | Boolean   | Do not create new launch and report to predefined Launch provided by UUID above. Default `true`.                                                                                                                                                                                                                                                                             | No           |
| rp.launch.uuid.print         | Boolean   | Enables printing Launch UUID on test run start. Default `false`.                                                                                                                                                                                                                                                                                                             | No           |
| rp.launch.uuid.print.output  | Enum      | Launch UUID print output. Default `stdout`. Possible values: `stderr`, `stdout`.                                                                                                                                                                                                                                                                                             | No           |
| rp.enable                    | Boolean   | Enable/Disable logging to ReportPortal: rp.enable=true - enable log to RP server. Any other value means 'false': rp.enable=false - disable log to RP server. If parameter is absent in  properties file then automation project results will be posted on RP.                                                                                                                | No           |
| rp.description               | String    | Launch description                                                                                                                                                                                                                                                                                                                                                           | No           |
| rp.attributes                | String    | Set of attributes for specifying additional meta information for current launch. Format: key:value;value;build:12345-6. Attributes should be separated by “;”, keys and values - “:”.                                                                                                                                                                                        | No           |
| rp.reporting.async           | Boolean   | Enables asynchronous reporting. Available values - `true` (by default) or `false`. Supported only in 5+ version.                                                                                                                                                                                                                                                             | No           |
| rp.reporting.callback        | Boolean   | Enables [callback reporting](https://github.com/reportportal/client-java/wiki/Callback-reporting-usefulness). Available values - `true` or `false`(by default). Supported only in 5+ vesion                                                                                                                                                                                  | No           |
| rp.rerun                     | Boolean   | Enables [rerun mode](https://reportportal.io/docs/developers-guides/RerunDevelopersGuide/). Available values - `true` or `false`(by default). Supported only in 5+ version                                                                                                                                                                                                   | No           |
| rp.rerun.of                  | String    | Specifies UUID of launch that has to be rerun.                                                                                                                                                                                                                                                                                                                               | No           |
| rp.convertimage              | Boolean   | Colored log images can be converted to grayscale for reducing image size. Values: ‘true’ – will be converted. Any other value means ‘false’.                                                                                                                                                                                                                                 | No           |
| rp.mode                      | Enum      | ReportPortal provides possibility to specify visibility of executing launch. Currently two modes are supported: DEFAULT - all users from project can see this launch; DEBUG - all users except of Customer role can see this launch (in debug sub tab). Note: for all java based clients (TestNG, Junit) mode will be set automatically to "DEFAULT" if it is not specified. | No           |
| rp.skipped.issue             | Boolean   | ReportPortal provides feature to mark skipped tests as not 'To Investigate' items on WS side. Parameter could be equal boolean values: <li>`true` - skipped tests considered as issues and will be marked as 'To Investigate' on ReportPortal. <li>`false` - skipped tests will not be marked as 'To Investigate' on application.                                            | No           |
| rp.batch.size.logs           | Integer   | Put logs into batches of specified size in order to rise up performance and reduce number of requests to server. Default = 10                                                                                                                                                                                                                                                | No           |
| rp.batch.payload.limit       | Long      | Limit batches by payload size to avoid request rejection due to server limitations.                                                                                                                                                                                                                                                                                          | No           |
| rp.rx.buffer.size            | Integer   | Internal queue size for log processing, increase this value along with log batch size if you see not all your logs passing to server. Default = 128                                                                                                                                                                                                                          | No           |
| rp.keystore.resource         | String    | Keystore file path to be used in HTTPS communication                                                                                                                                                                                                                                                                                                                         | No           |
| rp.keystore.password         | String    | Access password for certificate storage package, mentioned above                                                                                                                                                                                                                                                                                                             | No           |
| rp.keystore.type             | String    | Keystore type. Default: `JKS`                                                                                                                                                                                                                                                                                                                                                | No           |
| rp.truststore.resource       | String    | Truststore file path to be used in HTTPS communication                                                                                                                                                                                                                                                                                                                       | No           |
| rp.truststore.password       | String    | Access password for certificate storage package, mentioned above                                                                                                                                                                                                                                                                                                             | No           |
| rp.truststore.type           | String    | Truststore type. Default: `JKS`                                                                                                                                                                                                                                                                                                                                              | No           |


Launch name sets once before first execution, because in common launch parts are fixed for a long time. By keeping the
same launch name we will know a fixed list of suites behind it. That will allow us to have a history trend. On Report
Portal UI different launch iterations will be saved with postfix "\#number", like "Test Launch \#1", "Test Launch \#2"
etc.

#### Authentication

ReportPortal supports two authentication methods:

1. **API Key authentication** (default) - using `rp.api.key` parameter
2. **OAuth 2.0 Password Grant authentication** - using OAuth parameters (`rp.oauth.*`)

**Authentication priority:**
- If both API key and OAuth parameters are provided, OAuth 2.0 authentication will be used.
- Either API key or complete OAuth 2.0 configuration is required to connect to ReportPortal.

**OAuth 2.0 configuration example:**

```properties
rp.endpoint=https://reportportal.example.com/
rp.oauth.token.uri=https://reportportal.example.com/uat/sso/oauth/token
rp.oauth.username=my-username
rp.oauth.password=my-password
rp.oauth.client.id=client-id
rp.oauth.client.secret=client-id-secret
rp.oauth.scope=offline_access
rp.launch=launch-name
rp.project=project-name
```

> **Note:** `rp.oauth.client.secret` and `rp.oauth.scope` are optional parameters.

> If mandatory parameters are missed client will throw an [InternalReportPortalClientException](https://github.com/reportportal/client-java/blob/master/src/main/java/com/epam/reportportal/exception/InternalReportPortalClientException.java).

### Multi-process join parameters

| **Property name**                 | **Type** | **Description**                                                                                                                                                                                                                |
|-----------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| rp.client.join                    | Boolean  | Default: `true`<br /> Enable / Disable multi-process launch join mode                                                                                                                                                          |
| rp.client.join.mode               | Enum     | \[FILE, SOCKET], Default: `FILE`<br/> Which mechanism will be used to join multi-process launches:<br/> <li>`FILE` - the client will create a locking file<li>`SOCKET` - the client will open a socket                         |
| rp.client.join.port               | Integer  | Default: 25464<br>If client join mode set to `SOCKET`, this property controls port number of the socket                                                                                                                        |
| rp.client.join.timeout.value      | Integer  | Default: 1.8M milliseconds (30 minutes)<br> Timeout value for secondary launches. Primary launch will wait that amount of time after test execution for secondary launch finish.                                               |
| rp.client.join.timeout.unit       | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                        |
| rp.client.join.file.lock.name     | String   | Default: `reportportal.lock`<br>A name of a main lock file, can be an absolute path. A client which managed to obtain that lock count itself as a primary launch process. It rewrites synchronization file with its launch ID. |
| rp.client.join.file.sync.name     | String   | Default: `reportportal.sync`<br>A name of a launch ID synchronization file, can be an absolute path. Each client waits for a lock on that file to get a launch ID (first line) and write its own ID to the end of the file.    |
| rp.client.join.lock.timeout.value | Integer  | Default: 1 minute<br> Files lock / connection timeout for launches.                                                                                                                                                            |
| rp.client.join.lock.timeout.unit  | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                        |

### HTTP parameters

| **Property name**             | **Type** | **Description**                                                                                                                                                                                                                                          | 
|-------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| rp.http.proxy                 | String   | A URL of a HTTP proxy to connect to the endpoint.                                                                                                                                                                                                        |
| rp.http.proxy.username        | String   | A username for used proxy, works only if Proxy URL is set.                                                                                                                                                                                               |
| rp.http.proxy.password        | String   | Password for proxy, works only if Proxy URL and Proxy Username are set.                                                                                                                                                                                  |
| rp.http.logging               | Boolean  | Default: `false`<br> Enable / Disable HTTP logging.                                                                                                                                                                                                      |
| rp.http.timeout.call.value    | Integer  | Default: Infinitive<br> Timeout value for the entire call: resolving DNS, connecting, writing the request body, server processing, and reading the response body. If the call requires redirects or retries all must complete within one timeout period. |
| rp.http.timeout.call.unit     | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                  |
| rp.http.timeout.connect.value | Integer  | Default: 10 seconds<br> Connect timeout for new HTTP connections.                                                                                                                                                                                        |
| rp.http.timeout.connect.unit  | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                  |
| rp.http.timeout.read.value    | Integer  | Default: 10 seconds<br> Data read timeout for new HTTP connections.                                                                                                                                                                                      |
| rp.http.timeout.read.unit     | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                  |
| rp.http.timeout.write.value   | Integer  | Default: 10 seconds<br> Data write timeout for new HTTP connections.                                                                                                                                                                                     |
| rp.http.timeout.write.unit    | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                  |

### Truncation and sanitization parameters

| **Property name**              | **Type** | **Description**                                                                                    | 
|--------------------------------|----------|----------------------------------------------------------------------------------------------------|
| rp.truncation.field            | Boolean  | Default: `true`<br> Toggle certain field truncation to avoid API failures.                         |
| rp.truncation.replacement      | String   | Default: `...`<br> Replacement pattern for truncated fields                                        |
| rp.truncation.item.name.limit  | Integer  | Default: `1024`<br> Maximum item names length before truncation.                                   |
| rp.truncation.attribute.limit  | Integer  | Default: `128`<br> Maximum attribute key and value limit (counts separately)                       |
| rp.truncation.exception        | Boolean  | Default: `true`<br> Toggle Stack Trace truncation of exceptions that being logged to ReportPortal. |
| rp.attribute.limit             | Integer  | Default: `256`<br> Maximum number of attributes sent in request.                                   |
| rp.sanitization.replace.binary | Boolean  | Default: `true`<br> Toggle replacement of basic binary characters with \uFFFD char.                |

### Bug Tracking System parameters

| **Property name** | **Type** | **Description**                                                                                                                                                                                 | 
|-------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| rp.bts.project    | String   | Bug Tracking System Project name to use along with `@ExternalIssue` annotation. Should be the same as in corresponding integration.                                                             |
| rp.bts.url        | String   | Bug Tracking System base URL. Should be the same as in corresponding integration.                                                                                                               |
| rp.bts.issue.url  | String   | Bug Tracking System URL Pattern for Issues. Use <code>{issue_id}</code> and <code>{bts_project}</code> placeholders to mark a place where to put Issue ID and Bug Tracking System Project name. |
| rp.bts.issue.fail | Boolean  | Default: `true`<br> Fail tests marked with `@Issue` annotation if they passed. Designed to not miss the moment when the issue got fixed but test is still marked by annotation.                 |

## Proxy configuration

ReportPortal supports 2 options for setting Proxy configuration:

* JVM arguments (-Dhttps.proxyHost=localhost)
* `reportportal.properties` file

### JVM arguments

ReportPortal uses OkHttp as HTTP client, which can pick up JVM proxy settings. This is the most flexible and preferable
way to configure proxies, since it supports different proxy types. You can find out more about JVM proxies on
[Java networking and proxies](http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html) page.

### Properties file

If you need to set up just a simple HTTP proxy you can use `reportportal.properties` file. `rp.http.proxy` parameter
accepts HTTP proxy URL. This parameter can override JVM proxy arguments, so watch your back.

## Client usage

### When to use the client

This library is **not** an end-to-end integration with a test framework. If you use a common framework (JUnit 4/5, TestNG, Cucumber, etc.) prefer one of the ready-made **agents** which handle the full lifecycle for you. You can find the full list of supported Java agents by searching for the `agent-java-` prefix on our GitHub organization page: <https://github.com/reportportal?q=agent-java-&type=all>.

Use the Client directly only if:
* you need to report data from a **custom** or a **rare** test framework which is not covered by an existing agent;
* you are **extending** an existing agent and need to send extra items, logs or attachments;
* you are writing an auxiliary listener (for example, a browser / WebDriver / HTTP step logger) which runs together
  with an already running agent.

There are two typical ways to work with the client:

1. **Simplified access** — reuse a `Launch` instance that is already created and managed by an agent. This is the way
   to go for listeners, loggers and custom steps running inside a test which is already reported by an agent.
2. **Direct instantiation** — build a `ReportPortal` object, start a `Launch` from scratch, and drive the whole
   reporting lifecycle yourself. This is what you need when you integrate a custom or exotic framework.

### Simplified usage: reuse the Launch created by an agent

When a ReportPortal agent is active, it stores the current `Launch` instance in a thread-local-like holder. You can
retrieve it from anywhere in your code via the static method `Launch.currentLaunch()`:

```java
import com.epam.reportportal.service.Launch;

Launch launch = Launch.currentLaunch();
if (launch == null || launch == Launch.NOOP_LAUNCH) {
    return; // no active ReportPortal agent, nothing to do
}
```

The `NOOP_LAUNCH` check is important: when ReportPortal is disabled (for example, via `rp.enable=false` or because of a
misconfiguration), the agent installs a no-op instance instead of throwing an exception. Your listener must gracefully
skip reporting in such case.

Once you have a `Launch`, you can:

* inspect runtime configuration via `launch.getParameters()` (returns `ListenerParameters`);
* access the low-level REST client via `launch.getClient()` (returns `ReportPortalClient`);
* obtain the current Launch UUID via `launch.getLaunch()` (returns `Maybe<String>`);
* reuse the current-item stack via `launch.getStepReporter()` (returns `StepReporter`);
* start / finish nested items on the current item via `launch.startTestItem(...)` / `launch.finishTestItem(...)`;
* send logs via `ReportPortal.emitLog(...)`.

#### Example 1. Reporting the current Launch URL

Useful in a CI log, typically called from an `@AfterClass` / `@AfterSuite` hook:

```java
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;

import static java.util.Optional.ofNullable;

Launch launch = ofNullable(Launch.currentLaunch())
        .filter(l -> l != Launch.NOOP_LAUNCH)
        .orElseThrow(() -> new IllegalStateException("Launch not found"));

ListenerParameters parameters = launch.getParameters();
String launchUuid = launch.getLaunch().blockingGet();
String baseUrl = parameters.getBaseUrl();
baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
LOGGER.info("Launch URL: {}/ui/#{}/launches/all/{}", baseUrl, parameters.getProjectName(), launchUuid);
```

#### Example 2. Resolving numeric Launch ID via the REST client

`launch.getClient()` exposes the same Retrofit-based REST client that the agent uses internally, so you can call any
ReportPortal API method without creating another HTTP client:

```java
ofNullable(Launch.currentLaunch()).ifPresent(l -> {
    String launchUuid = l.getLaunch().blockingGet();
    LaunchResource info = l.getClient().getLaunchByUuid(launchUuid).blockingGet();
    LOGGER.info("Launch ID: {}", info != null ? info.getLaunchId() : null);
});
```

> **Note:** REST calls may throw `retrofit2.HttpException` if the launch is not yet visible on the server side in async
> mode. Wrap such calls in a small retry loop if you depend on their result.

#### Example 3. Reporting custom steps and attachments from a listener

The `StepReporter` obtained via `launch.getStepReporter()` maintains its own stack of "virtual" steps which are nested
inside the current test item of the agent. It is the recommended entry point for any custom step / log / screenshot
reporting — for instance, a Selenide, Selenium, REST Assured or HTTP client listener:

```java
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.files.ByteSource;

import java.time.Instant;

public void beforeEvent(String stepName) {
    ofNullable(Launch.currentLaunch())
            .ifPresent(l -> l.getStepReporter().sendStep(ItemStatus.INFO, stepName));
}

public void afterEvent(boolean passed, byte[] screenshotPng) {
    ofNullable(Launch.currentLaunch()).ifPresent(l -> {
        if (!passed) {
            ReportPortal.emitLog(
                    new ReportPortalMessage(ByteSource.wrap(screenshotPng), "image/png", "Screenshot"),
                    LogLevel.ERROR.name(),
                    Instant.now());
            l.getStepReporter().finishPreviousStep(ItemStatus.FAILED);
        } else {
            l.getStepReporter().finishPreviousStep();
        }
    });
}
```

Key points of the `StepReporter` API:

* `sendStep(ItemStatus, String)` — starts and immediately schedules to finish a virtual step with the given status;
* `finishPreviousStep()` / `finishPreviousStep(ItemStatus)` — closes the previous virtual step, optionally overriding
  its status (useful when the step outcome becomes known only after the next event arrives);
* `ReportPortal.emitLog(...)` — sends a log / attachment for the **currently active** test item, whether it is a
  "real" agent item or a virtual step. Use it for screenshots, page sources, request / response dumps, etc.

Because `currentLaunch()` works across threads, the same listener can be plugged into any framework without knowing
which agent is running underneath, as long as that agent uses this client library (which all official
`agent-java-*` integrations do).

### Direct usage: instantiate the client from scratch

If you are writing a custom framework integration, you need to build the client yourself and drive the launch
lifecycle end-to-end. The canonical recipe is used by every official agent (see
[`agent-java-junit5`](https://github.com/reportportal/agent-java-junit5/blob/develop/src/main/java/com/epam/reportportal/junit5/ReportPortalExtension.java)
and
[`agent-java-testNG`](https://github.com/reportportal/agent-java-testNG/blob/develop/src/main/java/com/epam/reportportal/testng/TestNGService.java)).

#### Step 1. Build a `ReportPortal` instance

`ReportPortal.builder().build()` reads all `rp.*` parameters from the sources described above (JVM args, environment
variables, `reportportal.properties`). You usually create a **single shared** instance per JVM:

```java
import com.epam.reportportal.service.ReportPortal;

public static final ReportPortal REPORT_PORTAL = ReportPortal.builder().build();
```

The builder accepts optional overrides if you need them:

```java
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import okhttp3.OkHttpClient;

import java.util.concurrent.Executors;

ListenerParameters params = new ListenerParameters();
params.setBaseUrl("https://reportportal.example.com/");
params.setApiKey("YOUR-API-KEY");
params.setProjectName("my-project");
params.setLaunchName("My Custom Launch");
params.setEnable(true);

ReportPortal rp = ReportPortal.builder()
        .withParameters(params)
        .withHttpClient(new OkHttpClient.Builder())
        .withExecutorService(Executors.newFixedThreadPool(4))
        .build();
```

If you already have a fully configured low-level `ReportPortalClient` (for example, when you write tests for the
client itself), use the static factory methods `ReportPortal.create(client, params)` instead of the builder.

#### Step 2. Start a `Launch`

A `Launch` object is a reactive wrapper around the REST API which batches requests, retries failures and finishes
in the correct order:

```java
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;

import java.time.Instant;

ListenerParameters params = rp.getParameters();

StartLaunchRQ startRq = new StartLaunchRQ();
startRq.setName(params.getLaunchName());
startRq.setDescription(params.getDescription());
startRq.setStartTime(Instant.now());
startRq.setMode(params.getLaunchRunningMode());
startRq.setAttributes(params.getAttributes());
startRq.setRerun(params.isRerun());
if (params.getRerunOf() != null && !params.getRerunOf().isBlank()) {
    startRq.setRerunOf(params.getRerunOf());
}

Launch launch = rp.newLaunch(startRq);
Maybe<String> launchId = launch.start(); // returns a promise; the request is sent asynchronously
```

> **Tip:** Always register a JVM shutdown hook to finish the launch even when the process is terminated abnormally.
> Agents do it like this:
>
> ```java
> Runtime.getRuntime().addShutdownHook(new Thread(() -> {
>     FinishExecutionRQ finishRq = new FinishExecutionRQ();
>     finishRq.setEndTime(Instant.now());
>     launch.finish(finishRq);
> }));
> ```

If you want to **report into an existing launch** (for example, a launch started by a primary process in a
multi-module build), use `ReportPortal.withLaunch(Maybe<String> launchUuid)` instead of `newLaunch(...)`. Do not call
`launch.start()` in that case, and do not send `finish` either — the primary process owns the lifecycle.

#### Step 3. Start and finish test items

Test items form a tree: suites contain tests, tests contain steps, steps may contain nested steps. Each call returns
a `Maybe<String>` promise with the item UUID which you pass to children and to the finish request:

```java
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import io.reactivex.Maybe;

StartTestItemRQ suiteRq = new StartTestItemRQ();
suiteRq.setName("My Suite");
suiteRq.setType("SUITE");
suiteRq.setStartTime(Instant.now());
Maybe<String> suiteId = launch.startTestItem(suiteRq);

StartTestItemRQ stepRq = new StartTestItemRQ();
stepRq.setName("My first test");
stepRq.setType("STEP");
stepRq.setCodeRef("com.example.MyTest.firstTest");
stepRq.setStartTime(Instant.now());
Maybe<String> stepId = launch.startTestItem(suiteId, stepRq);

// ... run the test, report logs, attachments, nested steps ...

FinishTestItemRQ finishStep = new FinishTestItemRQ();
finishStep.setStatus(ItemStatus.PASSED.name());
finishStep.setEndTime(Instant.now());
launch.finishTestItem(stepId, finishStep);

FinishTestItemRQ finishSuite = new FinishTestItemRQ();
finishSuite.setEndTime(Instant.now());
launch.finishTestItem(suiteId, finishSuite);
```

Allowed item types: `SUITE`, `STORY`, `TEST`, `SCENARIO`, `STEP`, `BEFORE_CLASS`, `BEFORE_GROUPS`, `BEFORE_METHOD`,
`BEFORE_SUITE`, `BEFORE_TEST`, `AFTER_CLASS`, `AFTER_GROUPS`, `AFTER_METHOD`, `AFTER_SUITE`, `AFTER_TEST`.

Item statuses are values of the `com.epam.reportportal.listeners.ItemStatus` enum: `PASSED`, `FAILED`, `SKIPPED`,
`STOPPED`, `INTERRUPTED`, `CANCELLED`, `INFO`, `WARN`.

#### Step 4. Send logs and attachments

Logs are attached to the currently active item (determined automatically from the thread that emits them):

```java
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.files.ByteSource;

import java.time.Instant;

ReportPortal.emitLog("My test started", LogLevel.INFO.name(), Instant.now());

byte[] screenshotPng = takeScreenshot();
ReportPortal.emitLog(
        new ReportPortalMessage(ByteSource.wrap(screenshotPng), "image/png", "Screenshot on failure"),
        LogLevel.ERROR.name(),
        Instant.now());
```

To log stack traces from caught exceptions use the helper `ReportPortal.sendStackTraceToRP(throwable)`.

#### Step 5. Finish the launch

Always call `launch.finish(...)` after the last item is finished — it flushes pending batches, waits for in-flight
requests and releases resources:

```java
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;

FinishExecutionRQ finishRq = new FinishExecutionRQ();
finishRq.setEndTime(Instant.now());
launch.finish(finishRq);
```

#### Threading and asynchrony notes

* All `startTestItem` / `finishTestItem` / `log` / `emitLog` calls are **non-blocking** — they return immediately and
  schedule the actual HTTP request on the internal `RxJava` scheduler. This means exceptions from the server do not
  propagate back to your code; instead they are logged at `WARN` / `ERROR` level, in accordance with the rules
  described in `AGENTS.md`. Do not call `blockingGet()` on the returned `Maybe` in hot paths.
* `ReportPortal.emitLog(...)` picks the currently active item from the calling thread. If you emit logs from a worker
  thread that was not spawned by the agent, make sure the parent item is still open at that moment.
* When `rp.reporting.async` is `false`, requests are still asynchronous on the client side but are issued
  synchronously on the server — useful for tests of the client itself.
* If you need to correlate multiple independent processes into the same launch, enable the multi-process join
  parameters (see the **Multi-process join parameters** section above) — no extra code changes are required.

For more elaborate, production-grade examples study the source code of the official agents — they are the best
reference for edge cases (retries, reruns, callback reporting, test item tree tracking, etc.):

* [`agent-java-junit5` — ReportPortalExtension](https://github.com/reportportal/agent-java-junit5/blob/develop/src/main/java/com/epam/reportportal/junit5/ReportPortalExtension.java)
* [`agent-java-testNG` — TestNGService](https://github.com/reportportal/agent-java-testNG/blob/develop/src/main/java/com/epam/reportportal/testng/TestNGService.java)
* [`logger-java-selenide` — listener that reuses the current `Launch`](https://github.com/reportportal/logger-java-selenide/blob/master/src/main/java/com/epam/reportportal/selenide/ReportPortalSelenideEventListener.java)
