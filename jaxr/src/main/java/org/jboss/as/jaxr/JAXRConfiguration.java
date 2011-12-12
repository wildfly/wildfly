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

import org.jboss.msc.service.ServiceName;

/**
 * The configuration of the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public class JAXRConfiguration {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jaxr", "configuration");

    public static String[] REQUIRED_ATTRIBUTES = new String[]{
            ModelConstants.CONNECTION_FACTORY,
            ModelConstants.PUBLISH_URL,
            ModelConstants.QUERY_URL
    };

    // Context to which JAXR ConnectionFactory to bind to
    private String connectionFactoryBinding;
    // The jUDDI server publish URL
    private String publishURL;
    // The jUDDI server publish URL
    private String queryURL;

    public JAXRConfiguration() {
        init();
    }

    public void init() {
        connectionFactoryBinding = null;
    }

    public void applyUpdateToConfig(String attributeName, String attributeValue) {
        if (attributeValue != null) {
            if (attributeName.equals(ModelConstants.CONNECTION_FACTORY)) {
                setConnectionFactoryBinding(attributeValue);
            } else if (attributeName.equals(ModelConstants.PUBLISH_URL)) {
                setPublishURL(attributeValue);
            } else if (attributeName.equals(ModelConstants.QUERY_URL)) {
                setQueryURL(attributeValue);
            } else {
                throw new IllegalArgumentException("Invalid attribute name: " + attributeName);
            }
        }
    }

    public String getConnectionFactoryBinding() {
        return connectionFactoryBinding;
    }

    public void setConnectionFactoryBinding(String connectionFactoryBinding) {
        this.connectionFactoryBinding = connectionFactoryBinding;
    }

    public String getPublishURL() {
        return publishURL;
    }

    public void setPublishURL(String publishURL) {
        this.publishURL = publishURL;
    }

    public String getQueryURL() {
        return queryURL;
    }

    public void setQueryURL(String queryURL) {
        this.queryURL = queryURL;
    }
}
