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
package org.wildfly.extension.picketlink.federation.service;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.picketlink.config.federation.KeyProviderType;
import org.picketlink.config.federation.STSType;
import org.wildfly.extension.picketlink.federation.FederationExtension;
import org.wildfly.extension.picketlink.federation.config.IDPConfiguration;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationService implements Service<FederationService> {

    private static final String SERVICE_NAME = "FederationService";
    private volatile KeyProviderType keyProviderType;
    private volatile STSType stsType;
    private volatile IDPConfiguration idpConfiguration;
    private final String alias;

    public FederationService(String alias) {
        this.alias = alias;
    }

    public static ServiceName createServiceName(String alias) {
        return ServiceName.JBOSS.append(FederationExtension.SUBSYSTEM_NAME, SERVICE_NAME, alias);
    }

    @Override
    public FederationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    KeyProviderType getKeyProviderType() {
        return this.keyProviderType;
    }

    void setKeyProviderType(final KeyProviderType keyProviderType) {
        this.keyProviderType = keyProviderType;
    }

    void setSTSType(final STSType STSType) {
        this.stsType = STSType;
    }

    STSType getStsType() {
        return this.stsType;
    }

    IDPConfiguration getIdpConfiguration() {
        return this.idpConfiguration;
    }

    void setIdpConfiguration(final IDPConfiguration idpConfiguration) {
        this.idpConfiguration = idpConfiguration;
    }

    public String getAlias() {
        return this.alias;
    }
}
