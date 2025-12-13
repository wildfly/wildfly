/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * For anyone developing tests in this package, here are quick tips:
 *
 * There is only one WF container available for all these tests.
 * Tests are running from "wildfly-1" distribution directory.
 * Default profile is "standalone.xml" so be aware that HA configuration is different, e.g. local caches
 * For ServerSetupTask, do not provide any targetContainers (and that container is not NODE_1)
 *
 * Most importantly, to run a single test you need to exclude the other executions, just use:
 *
 * mvn clean install -P="-ts.clustering.cluster.ha.profile,-ts.clustering.cluster.fullha.profile,-ts.clustering.cluster.ha-infinispan-server.profile,-ts.clustering.single.testable.profile" -Dtest=...
 *
 * @author Radoslav Husar
 */
package org.jboss.as.test.clustering.single;
