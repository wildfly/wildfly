/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * @author Radoslav Husar
 */
@ApplicationPath("/app")
public class FaultTolerantApplication extends Application {
}
