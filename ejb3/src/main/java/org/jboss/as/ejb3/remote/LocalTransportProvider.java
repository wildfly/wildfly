/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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


package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.ejb.client.EJBReceiver;
import org.jboss.ejb.client.EJBReceiverContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *  @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class LocalTransportProvider implements EJBTransportProvider, Service<LocalTransportProvider> {

    public static final ServiceName DEFAULT_LOCAL_TRANSPORT_PROVIDER_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("default-local-transport-provider");

    public static final ServiceName BY_VALUE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localTransportProvider", "value");
    public static final ServiceName BY_REFERENCE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "localTransportProvider", "reference");

    private final boolean allowPassByReference;

    private final InjectedValue<DeploymentRepository> deploymentRepository = new InjectedValue<>();

    private volatile LocalEjbReceiver receiver;

    public LocalTransportProvider(final boolean allowPassByReference){
        this.allowPassByReference = allowPassByReference;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        receiver = new LocalEjbReceiver(allowPassByReference, deploymentRepository.getValue());
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    public boolean supportsProtocol(final String uriScheme) {
        switch (uriScheme) {
            case "local": {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    public EJBReceiver getReceiver(final EJBReceiverContext receiverContext, final String uriScheme) throws IllegalArgumentException {
        switch (uriScheme) {
            case "local" : {
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported EJB receiver protocol " + uriScheme);
            }
        }
        return receiver;
    }

    @Override
    public LocalTransportProvider getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Injector<DeploymentRepository> getDeploymentRepository() {
        return deploymentRepository;
    }
}
