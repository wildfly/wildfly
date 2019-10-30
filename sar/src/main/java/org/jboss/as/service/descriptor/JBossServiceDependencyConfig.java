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

package org.jboss.as.service.descriptor;

import java.io.Serializable;

/**
 * Configuration for a service dependency.
 *
 * @author John E. Bailey
 */
public class JBossServiceDependencyConfig implements Serializable {
    private static final long serialVersionUID = 7058092116435789802L;

    private String dependencyName;
    private JBossServiceConfig serviceConfig;
    private String proxyType;
    private String optionalAttributeName;

    public String getDependencyName() {
        return dependencyName;
    }

    public void setDependencyName(String dependencyName) {
        this.dependencyName = dependencyName;
    }

    public JBossServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(JBossServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getOptionalAttributeName() {
        return optionalAttributeName;
    }

    public void setOptionalAttributeName(String optionalAttributeName) {
        this.optionalAttributeName = optionalAttributeName;
    }
}
