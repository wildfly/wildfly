/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.datasource;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.AbstractPlatformBindingProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Processor responsible for binding the default datasource to the naming context of EE modules/components.
 *
 * @author Eduardo Martins
 */
public class DefaultDataSourceBindingProcessor extends AbstractPlatformBindingProcessor {

    public static final String DEFAULT_DATASOURCE_JNDI_NAME = "DefaultDataSource";
    public static final String COMP_DEFAULT_DATASOURCE_JNDI_NAME = "java:comp/"+DEFAULT_DATASOURCE_JNDI_NAME;

    @Override
    protected void addBindings(DeploymentUnit deploymentUnit, EEModuleDescription moduleDescription) {
        final String defaultDataSource = moduleDescription.getDefaultResourceJndiNames().getDataSource();
        if(defaultDataSource != null) {
            addBinding(defaultDataSource, DEFAULT_DATASOURCE_JNDI_NAME, deploymentUnit, moduleDescription);
        }
    }
}
