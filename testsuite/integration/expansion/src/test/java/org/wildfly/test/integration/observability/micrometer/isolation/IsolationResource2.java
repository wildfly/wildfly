/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.isolation;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/")
public class IsolationResource2 extends AbstractIsolationResource {

    public static final String DEPLOYMENT_NAME = "service-two";

    public IsolationResource2() {
        super(DEPLOYMENT_NAME, "app2", "app1");
    }
}
