[![Maven Central](https://img.shields.io/maven-central/v/com.echobox/ebx-shutdown-sdk.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.echobox%22%20AND%20a:%22ebx-shutdown-sdk%22) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://raw.githubusercontent.com/ebx/ebx-shutdown-sdk/master/LICENSE) [![Build Status](https://travis-ci.org/ebx/ebx-shutdown-sdk.svg?branch=dev)](https://travis-ci.org/ebx/ebx-shutdown-sdk)
# ebx-shutdown-sdk

Listening for and responding gracefully to application shutdown is common boilerplate. Whilst it's
possible for applications to directly monitor for SIGTERM signals:

```
Runtime.getRuntime().addShutdownHook(new Thread(() -> { ...
```

experience has shown it only takes one dependency to do this incorrectly for the entire
application to close ungracefully. This SDK intends to provide an alternative way of
guaranteeing graceful shutdowns.

* `ShutdownMonitor` is an interface for requesting a shutdown, `setShutdownRequested` and
 checking for if a shutdown has been requested, `isShutdownRequested`.
* `ShutdownRequestedException` is provided as an optional way for handling resource cleanup during
 shutdown.
 
Implementations included are:

* `SimpleShutdownMonitor` - Supports manual triggering only.
* `WatchingShutdownMonitor` (and `WatchingShutdownMonitor.Builder`) - Extends
 `SimpleShutdownMonitor`. Can also be configured to listen for a shutdown file
  (`listenForShutdownFile`) and SIGTERM (`registerShutdownHook`).

## Installation

For our latest stable release use:

```
<dependency>
  <groupId>com.echobox</groupId>
  <artifactId>Shutdown</artifactId>
  <version>1.0.0</version>
</dependency>
```