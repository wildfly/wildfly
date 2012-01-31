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
package org.jboss.as.jaxr;

import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.jboss.msc.service.ServiceName;

/**
 * The configuration of the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 26-Oct-2011
 */
public class JAXRConfiguration {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jaxr", "configuration");

    public static String[] OPTIONAL_ATTRIBUTES = new String[]{
            ModelConstants.CONNECTION_FACTORY,
            ModelConstants.CONNECTION_FACTORY_IMPL
    };

    // Context to which JAXR ConnectionFactory to bind to
    private String connectionFactoryBinding;
    // Connection factory Implementation class
    private String connectionFactoryImplementation;
    // JAXR Properties
    private Properties properties = new Properties();

    public JAXRConfiguration() {
        init();
    }

    public void init() {
        connectionFactoryBinding = null;
    }

    public static ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(JAXRConstants.RESOURCE_NAME, locale != null ? locale : Locale.getDefault());
    }

    public void applyUpdateToConfig(String attributeName, String attributeValue) {
        if (attributeValue != null) {
            if (attributeName.equals(ModelConstants.CONNECTION_FACTORY)) {
                setConnectionFactoryBinding(attributeValue);
            } else if (attributeName.equals(ModelConstants.CONNECTION_FACTORY_IMPL)) {
                setConnectionFactoryImplementation(attributeValue);
            } else {
                properties.setProperty(attributeName, attributeValue);
            }
        }
    }

    public String getConnectionFactoryBinding() {
        return connectionFactoryBinding;
    }

    public void setConnectionFactoryBinding(String connectionFactoryBinding) {
        this.connectionFactoryBinding = connectionFactoryBinding;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setConnectionFactoryImplementation(String connectionFactoryImplementation) {
        this.connectionFactoryImplementation = connectionFactoryImplementation;
    }

    public String getConnectionFactoryImplementation() {
        return connectionFactoryImplementation;
    }

}
