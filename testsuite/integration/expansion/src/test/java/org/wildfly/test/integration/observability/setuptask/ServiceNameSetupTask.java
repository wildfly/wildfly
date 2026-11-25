/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.setuptask;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.AbstractSetupTask;

public class ServiceNameSetupTask extends AbstractSetupTask {
    public static final String SERVICE_NAME = "custom-service-name";
    public static final String ATTR_SERVICE_NAME = "service-name";
    protected static final String SUBSYSTEM_NAME = "opentelemetry";

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, ATTR_SERVICE_NAME, SERVICE_NAME));
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        executeOp(managementClient, clearAttribute(SUBSYSTEM_NAME, ATTR_SERVICE_NAME));
        ServerReload.reloadIfRequired(managementClient);
    }
}
