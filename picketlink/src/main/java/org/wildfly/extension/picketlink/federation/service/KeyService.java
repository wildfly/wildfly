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
public class KeyService implements Service<KeyService> {

    private static final String SERVICE_NAME = "KeyService";
    private final String keyName;
    private final InjectedValue<KeyStoreProviderService> keyStoreProviderService = new InjectedValue<KeyStoreProviderService>();
    private final String host;

    public KeyService(String keyName, String host) {
        this.keyName = keyName;
        this.host = host;
    }

    public static ServiceName createServiceName(final String federationAlias, String keyName) {
        return ServiceName.JBOSS.append(FederationExtension.SUBSYSTEM_NAME, SERVICE_NAME, federationAlias + ".keystore." + keyName);
    }

    @Override
    public KeyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        KeyStoreProviderService keyStoreProviderService = getKeyStoreProviderService().getValue();

        keyStoreProviderService.addKey(this.keyName, this.host);
    }

    @Override
    public void stop(StopContext context) {
        KeyStoreProviderService keyStoreProviderService = getKeyStoreProviderService().getValue();

        keyStoreProviderService.removeKey(this.keyName);
        context.getController().setMode(ServiceController.Mode.REMOVE);
    }

    public InjectedValue<KeyStoreProviderService> getKeyStoreProviderService() {
        return this.keyStoreProviderService;
    }
}
