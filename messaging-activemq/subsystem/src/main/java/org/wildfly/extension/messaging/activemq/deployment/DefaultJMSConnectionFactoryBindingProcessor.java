/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.injection.deployment.DefaultJMSConnectionFactoryBinding.DEFAULT_JMS_CONNECTION_FACTORY;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.AbstractPlatformBindingProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Processor responsible for binding the default Jakarta Messaging connection factory to the naming context of EE modules/components.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 * @author Eduardo Martins
 */
public class DefaultJMSConnectionFactoryBindingProcessor extends AbstractPlatformBindingProcessor {

    @Override
    protected void addBindings(DeploymentUnit deploymentUnit, EEModuleDescription moduleDescription) {
        final String defaultJMSConnectionFactory = moduleDescription.getDefaultResourceJndiNames().getJmsConnectionFactory();
        if(defaultJMSConnectionFactory != null) {
            addBinding(defaultJMSConnectionFactory, DEFAULT_JMS_CONNECTION_FACTORY, deploymentUnit, moduleDescription);
        }
    }
}
