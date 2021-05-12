/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.messaging.activemq.deployment;

import static org.wildfly.extension.messaging.activemq.deployment.DefaultJMSConnectionFactoryBinding.DEFAULT_JMS_CONNECTION_FACTORY;

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
