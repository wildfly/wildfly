/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.service;

import org.jboss.msc.service.ServiceName;

/**
 * The configuration of the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JAXRConfiguration {

    static final ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("jaxr", "as");

    public static final String DEFAULT_CONNECTIONFACTORY_BINDING = "java:/jaxr/ConnectionFactory";
    public static final String DEFAULT_DATASOURCE_BINDING = "java:jboss/datasources/ExampleDS";
    public static final boolean DEFAULT_DROPONSTART = false;
    public static final boolean DEFAULT_CREATEONSTART = false;
    public static final boolean DEFAULT_DROPONSTOP = false;

    // Datasource to Database
    private String dataSourceBinding = DEFAULT_DATASOURCE_BINDING;
    // Context to which JAXR ConnectionFactory to bind to
    private String connectionFactoryBinding = DEFAULT_CONNECTIONFACTORY_BINDING;
    // Should all tables be dropped on Start
    private boolean dropOnStart=DEFAULT_DROPONSTART;
    // Should all tables be created on Start
    private boolean createOnStart=DEFAULT_CREATEONSTART;
    // Should all tables be dropped on Stop
    private boolean dropOnStop=DEFAULT_DROPONSTOP;

    public static JAXRConfiguration INSTANCE = new JAXRConfiguration();

    // Hide ctor
    private JAXRConfiguration() {
    }

    public boolean isDropOnStop() {
        return dropOnStop;
    }

    public boolean isDropOnStart() {
        return dropOnStart;
    }

    public boolean isCreateOnStart() {
        return createOnStart;
    }

    public String getDataSourceBinding() {
        return dataSourceBinding;
    }

    public String getConnectionFactoryBinding() {
        return connectionFactoryBinding;
    }

    public void setConnectionFactoryBinding(String connectionFactoryBinding) {
        this.connectionFactoryBinding = connectionFactoryBinding;
    }

    public void setDataSourceBinding(String dataSourceBinding) {
        this.dataSourceBinding = dataSourceBinding;
    }

    public void setCreateOnStart(boolean createOnStart) {
        this.createOnStart = createOnStart;
    }

    public void setDropOnStart(boolean dropOnStart) {
        this.dropOnStart = dropOnStart;
    }

    public void setDropOnStop(boolean dropOnStop) {
        this.dropOnStop = dropOnStop;
    }
}
