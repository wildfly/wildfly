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

package org.jboss.as.test.integration.ejb.mdb.messagelistener.nomethodinterface;

import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
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
