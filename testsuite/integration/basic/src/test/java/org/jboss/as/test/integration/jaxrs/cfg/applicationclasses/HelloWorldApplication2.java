/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.cfg.applicationclasses;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Application with no path
 *
 * @author Stuart Douglas
 */
@ApplicationPath("app2")
public class HelloWorldApplication2 extends Application {
}
