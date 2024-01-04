/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.activationname.adapter;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivo Studensky
 */
public class SimpleResourceAdapter implements ResourceAdapter {

    private Map<SimpleActivationSpec, SimpleActivation> activations;

    public SimpleResourceAdapter() {
        this.activations = Collections.synchronizedMap(new HashMap<SimpleActivationSpec, SimpleActivation>());
    }

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) throws ResourceException {
        final String activationName = messageEndpointFactory.getActivationName();
        if (activationName == null) {
            throw new ResourceException("MessageEndpointFactory#getActivationName() cannot be null [WFLY-8074].");
        }
        SimpleActivation activation = new SimpleActivation(this, messageEndpointFactory, (SimpleActivationSpec) activationSpec);
        activations.put((SimpleActivationSpec) activationSpec, activation);
        activation.start();
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
        SimpleActivation activation = activations.remove(activationSpec);
        if (activation != null) { activation.stop(); }
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
        return new XAResource[0];
    }

    @Override
    public int hashCode() {
        int result = 17;
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) { return false; }

        if (other == this) { return true; }

        if (!(other instanceof SimpleResourceAdapter)) { return false; }

        SimpleResourceAdapter obj = (SimpleResourceAdapter) other;
        boolean result = true;
        return result;
    }

    class SimpleActivation {
        private SimpleResourceAdapter ra;
        private SimpleActivationSpec spec;
        private MessageEndpointFactory endpointFactory;

        public SimpleActivation(SimpleResourceAdapter ra,
                                MessageEndpointFactory endpointFactory,
                                SimpleActivationSpec spec)
                throws ResourceException {
            this.ra = ra;
            this.endpointFactory = endpointFactory;
            this.spec = spec;
        }

        public SimpleActivationSpec getActivationSpec() {
            return spec;
        }

        public MessageEndpointFactory getMessageEndpointFactory() {
            return endpointFactory;
        }

        public void start() throws ResourceException {
        }

        public void stop() {
        }
    }
}
