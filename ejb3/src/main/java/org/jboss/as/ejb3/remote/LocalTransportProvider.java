/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.logging.EjbLogger;
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

    @Override
    public void close(EJBReceiverContext receiverContext) throws Exception {
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
                throw EjbLogger.ROOT_LOGGER.unsupportedEJBReceiverProtocol(uriScheme);
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
