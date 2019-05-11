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
