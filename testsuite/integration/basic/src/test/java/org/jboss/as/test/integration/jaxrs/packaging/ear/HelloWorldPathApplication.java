/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.packaging.ear;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Application with a predefined path
 *@author Stuart Douglas
 */
@ApplicationPath("/hellopath")
public class HelloWorldPathApplication extends Application {
}
