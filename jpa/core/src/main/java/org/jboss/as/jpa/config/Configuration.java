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

package org.jboss.as.jpa.config;

/**
 * configuration properties that may appear in persistence.xml
 *
 * @author Scott Marlow
 */
public class Configuration {
    /**
     * name of the AS module that contains the persistence provider
     */
    public static final String PROVIDER_MODULE = "jboss.as.jpa.providerModule";

    /**
     * default if no PROVIDER_MODULE is specified.
     */
    public static final String PROVIDER_MODULE_DEFAULT = "org.hibernate";

    /**
     * default provider class
     */
    public static final String PROVIDER_CLASS_DEFAULT = "org.hibernate.ejb.HibernatePersistence";

    /**
     * if the PROVIDER_MODULE is this value, it is expected that the application has its own provider
     * in the deployment.
     */
    public static final String PROVIDER_MODULE_APPLICATION_SUPPLIED = "application";

    /**
     * Provider module that represents a bundled hibernate 3
     */
    public static final String PROVIDER_MODULE_HIBERNATE3_BUNDLED = "hibernate3-bundled";

    /**
     * Hibernate 3 persistence provider, if this provider is chosen. ADAPTER_MODULE_HIBERNATE3 will be enabled
     */
    public static final String PROVIDER_MODULE_HIBERNATE3 = "org.hibernate:3";

    /**
     * Hibernate 3 persistence provider adaptor
     */
    public static final String ADAPTER_MODULE_HIBERNATE3 = "org.jboss.as.jpa.hibernate:3";

    /**
     * name of the AS module that contains the persistence provider adapter
     */
    public static final String ADAPTER_MODULE = "jboss.as.jpa.adapterModule";

    /**
     * default if no ADAPTER_MODULE is specified.
     */
    public static final String ADAPTER_MODULE_DEFAULT = "org.jboss.as.jpa.hibernate:4";

    /**
     * name of the persistence provider adapter class
     */
    public static final String ADAPTER_CLASS = "jboss.as.jpa.adapterClass";


}
