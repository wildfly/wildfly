/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component;

import org.jboss.metadata.javaee.spec.RemoteEnvironment;

/**
 * The environment as read from a deployment descriptor
 *
 * @author Stuart Douglas
 */
public class DeploymentDescriptorEnvironment {

    private final String defaultContext;

    private final RemoteEnvironment environment;

    public DeploymentDescriptorEnvironment(String defaultContext, RemoteEnvironment environment) {
        this.defaultContext = defaultContext;
        this.environment = environment;
    }

    public String getDefaultContext() {
        return defaultContext;
    }

    public RemoteEnvironment getEnvironment() {
        return environment;
    }
}
