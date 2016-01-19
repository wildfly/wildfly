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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class TrustDomainService implements Service<TrustDomainService> {

    private static final String SERVICE_NAME = "TrustDomainService";
    private final String domainName;
    private final InjectedValue<IdentityProviderService> identityProviderService = new InjectedValue<IdentityProviderService>();

    public TrustDomainService(String domainName) {
        this.domainName = domainName;
    }

    public static ServiceName createServiceName(String identityProviderName, String domainName) {
        return ServiceName.JBOSS.append(FederationExtension.SUBSYSTEM_NAME, SERVICE_NAME, identityProviderName + "." + domainName);
    }

    @Override
    public TrustDomainService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        getIdentityProviderService().getValue().addTrustedDomain(this.domainName);
    }

    @Override
    public void stop(StopContext context) {
        getIdentityProviderService().getValue().removeTrustedDomain(this.domainName);
        context.getController().setMode(ServiceController.Mode.REMOVE);
    }

    public InjectedValue<IdentityProviderService> getIdentityProviderService() {
        return this.identityProviderService;
    }
}
