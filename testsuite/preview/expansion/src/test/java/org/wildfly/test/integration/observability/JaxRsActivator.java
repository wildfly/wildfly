/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

// This is copied from testsuite/integration/microprofile/src/test/java/org/wildfly/test/integration/observability/JaxRsActivator.java
// this will be removed once promoted to ts/integ/mp
@ApplicationPath("/")
public class JaxRsActivator extends Application {

}
