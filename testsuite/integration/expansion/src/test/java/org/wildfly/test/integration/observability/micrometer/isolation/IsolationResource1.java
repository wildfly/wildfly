/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.isolation;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/")
public class IsolationResource1 extends AbstractIsolationResource {

    public static final String DEPLOYMENT_NAME = "service-one";

    public IsolationResource1() {
        super(DEPLOYMENT_NAME, "app1", "app2");
    }
}
