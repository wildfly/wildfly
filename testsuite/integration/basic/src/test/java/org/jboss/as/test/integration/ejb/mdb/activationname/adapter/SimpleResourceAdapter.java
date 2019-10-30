/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.mdb.activationname.adapter;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
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
