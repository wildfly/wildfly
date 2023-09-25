/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.metrics.application;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
@ApplicationPath("/metrics-app")
public class TestApplication extends Application {
}
