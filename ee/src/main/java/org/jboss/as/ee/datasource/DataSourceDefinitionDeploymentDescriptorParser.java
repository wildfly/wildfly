/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;
import org.jboss.metadata.javaee.spec.PropertyMetaData;

import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * Deployment processor responsible for processing data-source deployment descriptor elements
 *
 * @author Stuart Douglas
 */
public class DataSourceDefinitionDeploymentDescriptorParser extends AbstractDeploymentDescriptorBindingsProcessor {

    private static final String[] EMPTY_STRING_ARRAY = {};

    @Override
    protected List<BindingConfiguration> processDescriptorEntries(final DeploymentUnit deploymentUnit, final DeploymentDescriptorEnvironment environment, final ResourceInjectionTarget resourceInjectionTarget, final ComponentDescription componentDescription, final ClassLoader classLoader, final DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        DataSourcesMetaData dataSources = environment.getEnvironment().getDataSources();
        if(dataSources != null) {
            List<BindingConfiguration> ret = new ArrayList<BindingConfiguration>(dataSources.size());
            for(DataSourceMetaData dataSource : dataSources) {
                ret.add(getBindingConfiguration(dataSource));
            }
            return ret;
        }
        return Collections.emptyList();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private BindingConfiguration getBindingConfiguration(final DataSourceMetaData dataSource) {

        String name = dataSource.getName();
        if (name == null || name.isEmpty()) {
            throw MESSAGES.elementAttributeMissing("<data-source>", "name");
        }
        // if the name doesn't have a namespace then it defaults to java:comp/env
        if (!name.startsWith("java:")) {
            name = "java:comp/env/" + name;
        }

        final String className = dataSource.getClassName();
        if (className == null || className.equals(Object.class.getName())) {
            throw MESSAGES.elementAttributeMissing("<data-source>", "className");
        }

        final String[] properties;
        if(dataSource.getProperties() == null) {
            properties = EMPTY_STRING_ARRAY;
        } else {
            properties = new String[dataSource.getProperties().size()];
            final Iterator<PropertyMetaData> it = dataSource.getProperties().iterator();
            for(int i = 0; i < properties.length; ++i) {
                final PropertyMetaData prop = it.next();
                properties[i] = prop.getName() + "=" + prop.getValue();
            }
        }


        final DirectDataSourceInjectionSource directDataSourceInjectionSource = new DirectDataSourceInjectionSource();
        directDataSourceInjectionSource.setClassName(className);
        directDataSourceInjectionSource.setDatabaseName(dataSource.getDatabaseName());
        if (dataSource.getDescriptions() != null) {
            directDataSourceInjectionSource.setDescription(dataSource.getDescriptions().toString());
        }
        directDataSourceInjectionSource.setInitialPoolSize(dataSource.getInitialPoolSize());
        if (dataSource.getIsolationLevel() != null) {
            directDataSourceInjectionSource.setIsolationLevel(dataSource.getIsolationLevel().ordinal());
        }
        directDataSourceInjectionSource.setLoginTimeout(dataSource.getLoginTimeout());
        directDataSourceInjectionSource.setMaxIdleTime(dataSource.getMaxIdleTime());
        directDataSourceInjectionSource.setMaxStatements(dataSource.getMaxStatements());
        directDataSourceInjectionSource.setMaxPoolSize(dataSource.getMaxPoolSize());
        directDataSourceInjectionSource.setMinPoolSize(dataSource.getMinPoolSize());
        directDataSourceInjectionSource.setPassword(dataSource.getPassword());
        directDataSourceInjectionSource.setPortNumber(dataSource.getPortNumber());
        directDataSourceInjectionSource.setProperties(properties);
        directDataSourceInjectionSource.setServerName(dataSource.getServerName());
        directDataSourceInjectionSource.setTransactional(dataSource.getTransactional());
        directDataSourceInjectionSource.setUrl(dataSource.getUrl());
        directDataSourceInjectionSource.setUser(dataSource.getUser());


        final BindingConfiguration bindingDescription = new BindingConfiguration(name, directDataSourceInjectionSource);
        return bindingDescription;
    }

}
