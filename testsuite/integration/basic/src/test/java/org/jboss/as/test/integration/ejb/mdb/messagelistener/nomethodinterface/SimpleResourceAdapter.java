/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagelistener.nomethodinterface;

import java.lang.reflect.Method;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * @author Jan Martiska
 */
public class SimpleResourceAdapter implements ResourceAdapter {

    private MessageEndpoint endpoint;

    private Logger log = Logger.getLogger(SimpleResourceAdapter.class);

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        log.trace("SimpleResourceAdapter started");
    }

    @Override
    public void stop() {
        log.trace("SimpleResourceAdapter stopped");
    }

    /**
     * Send a message to the MDB right after the MDB endpoint is activated.
     * Using reflection to pick a method to invoke - see EJB 3.2 spec section 5.4.3
     */
    @Override
    public void endpointActivation(MessageEndpointFactory messageEndpointFactory,
                                   ActivationSpec activationSpec) throws ResourceException {
        log.trace("SimpleResourceAdapter activating MDB endpoint and sending a message to it");
        Class<?> endpointClass = messageEndpointFactory.getEndpointClass();
        try {
            Method methodToInvoke = endpointClass.getMethod(((SimpleActivationSpec)activationSpec).getMethodName(), String.class);
            MessageEndpoint endpoint = messageEndpointFactory.createEndpoint(null);
            this.endpoint = endpoint;
            methodToInvoke.invoke(endpoint, "Hello world");
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory,
                                     ActivationSpec activationSpec) {
        log.trace("SimpleResourceAdapter deactivating MDB endpoint");
        if (endpoint != null) {
            endpoint.release();
            endpoint = null;
        }
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimpleResourceAdapter)) {
            return false;
        }

        SimpleResourceAdapter that = (SimpleResourceAdapter)o;

        return !(endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null);

    }

    @Override
    public int hashCode() {
        return endpoint != null ? endpoint.hashCode() : 0;
    }
}
