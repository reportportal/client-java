# Client java

> **DISCLAIMER**: We use Google Analytics for sending anonymous usage information such as agent's and client's names, and their versions
> after a successful launch start. This information might help us to improve both ReportPortal backend and client sides. It is used by the
> ReportPortal team only and is not supposed for sharing with 3rd parties.

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/client-java.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22client-java%22)
[![CI Build](https://github.com/reportportal/client-java/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/client-java/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/client-java/branch/develop/graph/badge.svg?token=IVTys0o4JT)](https://codecov.io/gh/reportportal/client-java)
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## JVM-based clients configuration

Copy your configuration from UI of ReportPortal at User Profile section

or

In order to start using an agent, user should configure property file
"reportportal.properties" in such format:

**reportportal.properties**

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

**Parameters**

User should provide next parameters to agent.

| **Property name**                 | **Type** | **Description**                                                                                                                                                                                                                                                                                                                                                              | **Required** |
|-----------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| rp.endpoint                       | String   | URL of web service, where requests should be send                                                                                                                                                                                                                                                                                                                            | Yes          |
| rp.api.key or rp.uuid             | String   | Api token of user                                                                                                                                                                                                                                                                                                                                                            | Yes          |
| rp.launch                         | String   | The unique name of Launch (Run). Based on that name a history of runs will be created for particular name                                                                                                                                                                                                                                                                    | Yes          |
| rp.project                        | String   | Project name to identify scope                                                                                                                                                                                                                                                                                                                                               | Yes          |
| rp.enable                         | Boolean  | Enable/Disable logging to Report Portal: rp.enable=true - enable log to RP server. Any other value means 'false': rp.enable=false - disable log to RP server. If parameter is absent in  properties file then automation project results will be posted on RP.                                                                                                               | No           |
| rp.description                    | String   | Launch description                                                                                                                                                                                                                                                                                                                                                           | No           |
| rp.attributes                     | String   | Set of attributes for specifying additional meta information for current launch. Format: key:value;value;build:12345-6. Attributes should be separated by “;”, keys and values - “:”.                                                                                                                                                                                        | No           |
| rp.reporting.async                | Boolean  | Enables asynchronous reporting. Available values - `true` or `false`(by default). Supported only in 5+ vesion                                                                                                                                                                                                                                                                | No           |
| rp.reporting.callback             | Boolean  | Enables [callback reporting](https://github.com/reportportal/client-java/wiki/Callback-reporting-usefulness). Available values - `true` or `false`(by default). Supported only in 5+ vesion                                                                                                                                                                                  | No           |
| rp.rerun                          | Boolean  | Enables [rerun mode](https://github.com/reportportal/documentation/blob/master/src/md/src/DevGuides/rerun.md). Available values - `true` or `false`(by default). Supported only in 5+ version                                                                                                                                                                                | No           |
| rp.rerun.of                       | String   | Specifies UUID of launch that has to be reruned                                                                                                                                                                                                                                                                                                                              | No           |
| rp.convertimage                   | Boolean  | Colored log images can be converted to grayscale for reducing image size. Values: ‘true’ – will be converted. Any other value means ‘false’.                                                                                                                                                                                                                                 | No           |
| rp.mode                           | Enum     | ReportPortal provides possibility to specify visibility of executing launch. Currently two modes are supported: DEFAULT - all users from project can see this launch; DEBUG - all users except of Customer role can see this launch (in debug sub tab). Note: for all java based clients (TestNG, Junit) mode will be set automatically to "DEFAULT" if it is not specified. | No           |
| rp.skipped.issue                  | Boolean  | ReportPortal provides feature to mark skipped tests as not 'To Investigate' items on WS side. Parameter could be equal boolean values: <li>`true` - skipped tests considered as issues and will be marked as 'To Investigate' on Report Portal. <li>`false` - skipped tests will not be marked as 'To Investigate' on application.                                           | No           |
| rp.batch.size.logs                | Integer  | Put logs into batches of specified size in order to rise up performance and reduce number of requests to server. Default = 10                                                                                                                                                                                                                                                | No           |
| rp.rx.buffer.size                 | Integer  | Internal queue size for log processing, increase this value along with log batch size if you see not all your logs passing to server. Default = 128                                                                                                                                                                                                                          | No           |
| rp.keystore.resource              | String   | Put your JKS file into resources and specify path to it                                                                                                                                                                                                                                                                                                                      | No           |
| rp.keystore.password              | String   | Access password for JKS (certificate storage) package, mentioned above                                                                                                                                                                                                                                                                                                       | No           |
| rp.client.join                    | Boolean  | Enable / Disable multi-process launch join mode                                                                                                                                                                                                                                                                                                                              | No           |
| rp.client.join.mode               | Enum     | \[FILE, SOCKET], Default: `FILE`<br/> Which mechanism will be used to join multi-process launches:<br/> <li>`FILE` - the client will create a locking file<li>`SOCKET` - the client will open a socket                                                                                                                                                                       | No           |
| rp.client.join.port               | Integer  | Default: 25464<br>If client join mode set to `SOCKET`, this property controls port number of the socket                                                                                                                                                                                                                                                                      | No           |
| rp.client.join.timeout.value      | Integer  | Default: 1.8M milliseconds (30 minutes)<br> Timeout value for secondary launches. Primary launch will wait that amount of time after test execution for secondary launch finish.                                                                                                                                                                                             | No           |
| rp.client.join.timeout.unit       | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                                                                                                                                      | No           |
| rp.client.join.file.lock.name     | String   | Default: `reportportal.lock`<br>A name of a main lock file, can be an absolute path. A client which managed to obtain that lock count itself as a primary launch process. It rewrites synchronization file with its launch ID.                                                                                                                                               | No           |
| rp.client.join.file.sync.name     | String   | Default: `reportportal.sync`<br>A name of a launch ID synchronization file, can be an absolute path. Each client waits for a lock on that file to get a launch ID (first line) and write its own ID to the end of the file.                                                                                                                                                  | No           |
| rp.client.join.lock.timeout.value | Integer  | Default: 1 minute<br> Files lock / connection timeout for launches.                                                                                                                                                                                                                                                                                                          | No           |
| rp.client.join.lock.timeout.unit  | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                                                                                                                                      | No           |
| rp.http.timeout.call.value        | Integer  | Default: Infinitive<br> Timeout value for the entire call: resolving DNS, connecting, writing the request body, server processing, and reading the response body. If the call requires redirects or retries all must complete within one timeout period.                                                                                                                     | No           |
| rp.http.timeout.call.unit         | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                                                                                                                                      | No           |
| rp.http.timeout.connect.value     | Integer  | Default: 10 seconds<br> Connect timeout for new HTTP connections.                                                                                                                                                                                                                                                                                                            | No           |
| rp.http.timeout.connect.unit      | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                                                                                                                                      | No           |
| rp.http.timeout.read.value        | Integer  | Default: 10 seconds<br> Data read timeout for new HTTP connections.                                                                                                                                                                                                                                                                                                          | No           |
| rp.http.timeout.read.unit         | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                                                                                                                                      | No           |
| rp.http.timeout.write.value       | Integer  | Default: 10 seconds<br> Data write timeout for new HTTP connections.                                                                                                                                                                                                                                                                                                         | No           |
| rp.http.timeout.write.unit        | Enum     | Default: `MILLISECONDS`<br> Timeout value time unit. Should be one of values from `java.util.concurrent.TimeUnit` class                                                                                                                                                                                                                                                      | No           |

Launch name can be edited once, and should be edited once, before first execution. As usual, parts of launches are fixed for a long time.
Keeping the same name for launch, here we will understand a fixed list of suites under launch, will help to have a history trend, and on UI
instances of the same launch will be saved with postfix "\#number", like "Test Launch \#1", "Test Launch \#2" etc.

> If mandatory properties are missed client throw exception
> IllegalArgumentException.

**Proxy configuration**

For clients using a standard java proxy mechanism. New to Java scripting?
try [Java networking and proxies](<http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html>) page.

Ways to set up properties:

a. reportportal.properties file

b. command line properties (-Dhttps.proxyHost=localhost)

**How to provide parameters**

There are several ways to load parameters. Be aware that higher order overrides previous. For example, properties from file can be
overridden by JVM variables

| Order | Source
|-------| ----------------------|
| 1     | Environment variables |
| 2     | JVM variables         |
| 3     | Properties file       |

Properties file should have name

> reportportal.properties
>

It can be situated on the class path (in the project directory). If listener can’t find properties file it throws FileNotFoundException. By
default “reportportal.properties” exists in the reportportall-client.jar, but user can create his own “reportportal.properties” file and put
in class path.

