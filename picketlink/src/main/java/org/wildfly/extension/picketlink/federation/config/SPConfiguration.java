/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.federation.config;

import org.picketlink.config.federation.SPType;

/**
 * <p> This class is responsible to store all information about a given Service Provider deployment. The state is populated with values from the
 * subsystem configuration. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 12, 2012
 */
public class SPConfiguration extends SPType implements ProviderConfiguration {

    private volatile String securityDomain;
    private final String alias;
    private boolean supportMetadata;

    public SPConfiguration(String alias) {
        this.alias = alias;
    }

    public void setPostBinding(boolean postBinding) {
        if (postBinding) {
            super.setBindingType("POST");
        } else {
            super.setBindingType("REDIRECT");
        }
    }

    public boolean isPostBinding() {
        return "POST".equalsIgnoreCase(getBindingType());
    }

    @Override
    public String getSecurityDomain() {
        return this.securityDomain;
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    public void setSupportMetadata(boolean supportMetadata) {
        this.supportMetadata = supportMetadata;
    }

    public boolean isSupportMetadata() {
        return supportMetadata;
    }
}
