/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;


import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
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
