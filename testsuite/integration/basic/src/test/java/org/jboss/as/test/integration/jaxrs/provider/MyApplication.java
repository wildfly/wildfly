/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.provider;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * A Jakarta RESTful Web Services application.
 *
 * @author Josef Cacek
 */
@ApplicationPath(MyApplication.APPLICATION_PATH)
public class MyApplication extends Application {
    public static final String APPLICATION_PATH = "/";
}
