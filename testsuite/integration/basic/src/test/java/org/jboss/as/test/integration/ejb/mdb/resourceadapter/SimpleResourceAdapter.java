/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;


import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

import org.jboss.logging.Logger;

/**
 * @author Jaikiran Pai
 */
public class SimpleResourceAdapter implements ResourceAdapter, Referenceable, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(SimpleResourceAdapter.class);

    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
            throws ResourceException {
        logger.trace("endpoint activation invoked for endpoint factory " + endpointFactory + " and activation spec " + spec);
        ResourceAdapterDeploymentTracker.INSTANCE.endpointActivationCalled();
    }

    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        logger.trace("endpoint de-activation invoked for endpoint factory " + endpointFactory + " and activation spec " + spec);
        ResourceAdapterDeploymentTracker.INSTANCE.endpointDeactivationCalled();
    }

    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return null;
    }

    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        logger.trace("resource adapter start invoked");
        try {
            /*
            *  This is a small delay to emulate startup time of an RA.
            *  I understand that according to the spec, this step of RA should not perform any
            *  heavy task, and app server could throw WorkRejectedException if it takes too long.
            *  However, this small delay should be good rough approximation of the total time the RA
            *  could take to bootstrap itself more so, to reliably repoduce the issue, as otherwise,
            *  the failure is reproducible only indeterministically.
            */
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ResourceAdapterDeploymentTracker.INSTANCE.endpointStartCalled();
    }

    public void stop() {
        logger.trace("resource adapter stop invoked");
        ResourceAdapterDeploymentTracker.INSTANCE.endpointStopCalled();
    }

    public Reference getReference() throws NamingException {
        return null;
    }

    public void setReference(Reference reference) {
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }
}
