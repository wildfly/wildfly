/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.messaging.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ResourceInjectionTarget;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.JMSConnectionFactoriesMetaData;
import org.jboss.metadata.javaee.spec.JMSConnectionFactoryMetaData;
import org.jboss.metadata.javaee.spec.JMSDestinationMetaData;
import org.jboss.metadata.javaee.spec.JMSDestinationsMetaData;
import org.jboss.metadata.javaee.spec.PropertiesMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

/**
 * Process jms-destination and jms-connection-factory from deployment descriptor.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class MessagingJMSDefinitionDeploymentProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, ResourceInjectionTarget resourceInjectionTarget, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        if (environment == null) {
            return Collections.emptyList();
        }

        RemoteEnvironment remoteEnvironment = environment.getEnvironment();
        List<BindingConfiguration> bindingConfigurations = new ArrayList<BindingConfiguration>();

        JMSDestinationsMetaData destinationsMetadata = remoteEnvironment.getJmsDestinations();
        if (destinationsMetadata != null) {
            for (JMSDestinationMetaData metadata : destinationsMetadata) {
                BindingConfiguration bindingConfiguration = processJMSDestinationDefinition(metadata);
                bindingConfigurations.add(bindingConfiguration);
            }
        }

        JMSConnectionFactoriesMetaData connectionFactoriesMetadata = remoteEnvironment.getJmsConnectionFactories();
        if (connectionFactoriesMetadata != null) {
            for (JMSConnectionFactoryMetaData metadata : connectionFactoriesMetadata) {
                BindingConfiguration bindingConfiguration = processJMSConnectionFactoryDefinition(metadata);
                bindingConfigurations.add(bindingConfiguration);
            }
        }

        return bindingConfigurations;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private BindingConfiguration processJMSDestinationDefinition(JMSDestinationMetaData metadata) {
        String name = metadata.getName();
        String interfaceName = metadata.getInterfaceName();
        DirectJMSDestinationInjectionSource source = new DirectJMSDestinationInjectionSource(name, interfaceName);
        source.setDestinationName(metadata.getDestinationName());
        source.setResourceAdapter(metadata.getResourceAdapter());
        source.setClassName(metadata.getClassName());
        PropertiesMetaData properties = metadata.getProperties();
        if (properties != null) {
            for (PropertyMetaData property : properties) {
                source.addProperty(property.getKey(), property.getValue());
            }
        }

        return new BindingConfiguration(name, source);
    }

    private BindingConfiguration processJMSConnectionFactoryDefinition(JMSConnectionFactoryMetaData metadata) {
        String name = metadata.getName();
        DirectJMSConnectionFactoryInjectionSource source = new DirectJMSConnectionFactoryInjectionSource(name);
        source.setInterfaceName(metadata.getInterfaceName());
        source.setClassName(metadata.getClassName());
        source.setResourceAdapter(metadata.getResourceAdapter());
        source.setUser(metadata.getUser());
        source.setPassword(metadata.getPassword());
        source.setClientId(metadata.getClientId());
        PropertiesMetaData properties = metadata.getProperties();
        if (properties != null) {
            for (PropertyMetaData property : properties) {
                source.addProperty(property.getKey(), property.getValue());
            }
        }
        source.setTransactional(metadata.isTransactional());
        source.setMaxPoolSize(metadata.getMaxPoolSize());
        source.setMinPoolSize(metadata.getMinPoolSize());

        return new BindingConfiguration(name, source);
    }
}
