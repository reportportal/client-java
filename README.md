# Client java
 [ ![Download](https://api.bintray.com/packages/epam/reportportal/client-java/images/download.svg) ](https://bintray.com/epam/reportportal/client-java/_latestVersion)
 
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![UserVoice](https://img.shields.io/badge/uservoice-vote%20ideas-orange.svg?style=flat)](https://rpp.uservoice.com/forums/247117-report-portal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## JVM-based clients configuration

Copy your configuration from UI of ReportPortal at [User Profile](<#user-profile>) section

or

In order to start using an agent, user should configure property file
"reportportal.properties" in such format:

**reportportal.properties**

```properties
rp.endpoint = https://rp.epam.com/
rp.api.key = 8967de3b-fec7-47bb-9dbc-2aa4ceab8b1e
rp.launch = launch-name
rp.project = project-name

## OPTIONAL PARAMETERS
rp.reporting.async = true
rp.reporting.callback = true
rp.enable = true
rp.description = My awesome launch
rp.attributes = key:value;value
rp.rerun = true
rp.rerun.of = ae586912-841c-48de-9391-50720c35dd5a
rp.convertimage = true
rp.mode = DEFAULT
rp.skipped.issue = true
rp.batch.size.logs = 20
rp.keystore.resource = <PATH_TO_YOUR_KEYSTORE>
rp.keystore.password = <PASSWORD_OF_YOUR_KEYSTORE>
```


**Parameters**

User should provide next parameters to agent.

| **Parameter**                                 | **Description**      | **Required**|
|-----------------------------------------------|----------------------|-------------|
|rp.endpoint                                    |URL of web service, where requests should be send |Yes |
|rp.api.key or rp.uuid                          |Api token of user |Yes |
|rp.launch                                      |The unique name of Launch (Run). Based on that name a history of runs will be created for particular name |Yes |
|rp.project                                     |Project name to identify scope |Yes |
|rp.enable                                      |Enable/Disable logging to Report Portal: rp.enable=true - enable log to RP server.  Any other value means 'false': rp.enable=false - disable log to RP server.  If parameter is absent in  properties file then automation project results will be posted on RP. |No |
|rp.description                                 |Launch description |No |
|rp.attributes                                  |Set of attributes for specifying additional meta information for current launch. Format: key:value;value;build:12345-6. Attributes should be separated by “;”, keys and values - “:”. |No |
|rp.reporting.async                             |Enables asynchronous reporting. Available values - `true` or `false`(by default). Supported only in 5+ vesion |No |
|rp.reporting.callback                          |Enables [callback reporting](https://github.com/reportportal/client-java/wiki/Callback-reporting-usefulness). Available values - `true` or `false`(by default). Supported only in 5+ vesion |No |
|rp.rerun                                       |Enables [rerun mode](https://github.com/reportportal/documentation/blob/master/src/md/src/DevGuides/rerun.md). Available values - `true` or `false`(by default). Supported only in 5+ version | No |
|rp.rerun.of                                    |Specifies UUID of launch that has to be reruned |No |
|rp.convertimage                                |Colored log images can be converted to grayscale for reducing image size. Values: ‘true’ – will be converted. Any other value means ‘false’. |No |
|rp.mode                                        |ReportPortal provides possibility to specify visibility of executing launch. Currently two modes are supported: DEFAULT  - all users from project can see this launch; DEBUG - all users except of Customer role can see this launch (in debug sub tab). Note: for all java based clients (TestNG, Junit) mode will be set automatically to "DEFAULT" if it is not specified. |No |
|rp.skipped.issue                               |ReportPortal provides feature to mark skipped tests as not 'To Investigate' items on WS side. Parameter could be equal boolean values: *TRUE* - skipped tests considered as issues and will be marked as 'To Investigate' on Report Portal. *FALSE* - skipped tests will not be marked as 'To Investigate' on application. |No |
|rp.batch.size.logs                             |In order to rise up performance and reduce number of requests to server. Default = 10 |No |
|rp.keystore.resource                           |Put your JKS file into resources and specify path to it | No|
|rp.keystore.password                           |Access password for JKS (certificate storage) package, mentioned above |No |

Launch name can be edited once, and should be edited once, before first
execution. As usual, parts of launches are fixed for a long time. Keeping the
same name for launch, here we will understand a fixed list of suites under
launch, will help to have a history trend, and on UI instances of the same
launch will be saved with postfix "\#number", like "Test Launch \#1", "Test
Launch \#2" etc.

>   If mandatory properties are missed client throw exception
>   IllegalArgumentException.

**Proxy configuration**

For clients using a standard java proxy mechanism. New to Java scripting? try [Java networking and proxies](<http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html>) page.

Ways to set up properties:

a. reportportal.properties file

b. command line properties (-Dhttps.proxyHost=localhost)

**How to provide parameters**

There are several ways to load parameters. Be aware that higher order overrides previous. 
For example, properties from file can be overridden by JVM variables

| Order | Source
|-------| ----------------------|
| 1     | Environment variables |
| 2     | JVM variables         |
| 3     | Properties file       |


Properties file should have name 

> reportportal.properties
>

It can be situated on the class path (in the project directory). 
If listener can’t find properties file it throws FileNotFoundException. 
By default “reportportal.properties” exists in the reportportall-client.jar, 
but user can create his own “reportportal.properties” file and put in class path.

