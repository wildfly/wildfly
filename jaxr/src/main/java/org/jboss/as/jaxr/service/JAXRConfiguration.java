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

/**
 * The configuration of the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JAXRConfiguration {

    public static final boolean DEFAULT_DROPONSTART = false;
    public static final boolean DEFAULT_CREATEONSTART = false;
    public static final boolean DEFAULT_DROPONSTOP = false;

    // Datasource to Database
    private String dataSourceBinding;
    // Context to which JAXR ConnectionFactory to bind to
    private String connectionFactoryBinding;
    // Should all tables be dropped on Start
    private boolean dropOnStart;
    // Should all tables be created on Start
    private boolean createOnStart;
    // Should all tables be dropped on Stop
    private boolean dropOnStop;

    public JAXRConfiguration() {
        init();
    }

    public void init() {
        dataSourceBinding = null;
        connectionFactoryBinding = null;
        dropOnStart = DEFAULT_DROPONSTART;
        createOnStart = DEFAULT_CREATEONSTART;
        dropOnStop = DEFAULT_DROPONSTOP;
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
