/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
 * [TODO] AS7-2278 JAXR configuration through the domain model
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JAXRConfiguration {

    static final ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("jaxr", "as");

    public static final String JAXR_DEFAULT_CONNECTION_FACTORY_BINDING = "java:/jaxr/ConnectionFactory";
    public static final String JAXR_DEFAULT_DATASOURCE_BINDING = "java:jboss/datasources/ExampleDS";

    // Should all tables be created on Start
    private boolean createOnStart=true;
    // Should all tables be dropped on Stop
    private boolean dropOnStop=true;
    // Should all tables be dropped on Start
    private boolean dropOnStart=true;
    // Datasource to Database
    private String dataSourceUrl= JAXR_DEFAULT_DATASOURCE_BINDING;
    // Alias to the registry
    private String registryOperator="RegistryOperator";
    // Should I bind a Context to which JaxrConnectionFactory bound
    private boolean bindJaxr=true;
    // Context to which JAXR ConnectionFactory to bind to
    private String namingContext = JAXR_DEFAULT_CONNECTION_FACTORY_BINDING;

    public static JAXRConfiguration INSTANCE = new JAXRConfiguration();

    // Hide ctor
    private JAXRConfiguration() {
    }

    boolean isDropOnStop() {
        return dropOnStop;
    }

    boolean isDropOnStart() {
        return dropOnStart;
    }

    String getDataSourceUrl() {
        return dataSourceUrl;
    }

    String getRegistryOperator() {
        return registryOperator;
    }

    boolean isBindJaxr() {
        return bindJaxr;
    }

    String getNamingContext() {
        return namingContext;
    }

    boolean isCreateOnStart() {
        return createOnStart;
    }
}
