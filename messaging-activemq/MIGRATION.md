# Migration Guide

The goal of this guide is to help migration from messaging (with HornetQ) subsystem to the new messaging-activemq subsystem.

# Domain Model

* management model starts at `1.0.0`
  * there is __no__ resource transformation from the `messaging-activemq` resources to the legacy `messaging` resources.
* extension module: `org.jboss.as.messaging` -> `org.wildfly.extension.messaging-activemq`
* server address: `/subsystem=messaging/hornetq-server=<name>` -> `/subsystem=messaging-activemq/server=<name>`

# XML

* namespace:
  * `urn:jboss:domain:messaging:3.0` -> `urn:jboss:domain:messaging-activemq:1.0`
  * `urn:jboss:messaging-deployment:1.0` -> `urn:jboss:messaging-activemq-deployment:1.0`

# Logging

* prefix: `WFLYMSG` -> `WFLYMSGAMQ`

# Data

* relative to `jboss.server.data.dir`
  * `messagingbindings/` -> `activemq/bindings/`
  * `messagingjournal/` -> `activemq/journal/`
  * `messaginglargemessages/` -> `activemq/largemessages/`
  * `messagingpaging/` -> `activemq/paging/`
