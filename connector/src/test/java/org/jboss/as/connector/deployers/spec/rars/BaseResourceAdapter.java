/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * BaseResourceAdapter
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>.
 * @version $Revision: $
 */
public class BaseResourceAdapter implements ResourceAdapter {
    private static Logger log = Logger.getLogger(BaseResourceAdapter.class);

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory a message endpoint factory instance.
     * @param spec an activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
        log.debug("call endpointActivation");

    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory a message endpoint factory instance.
     * @param spec an activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        log.debug("call endpointDeactivation");

    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs an array of ActivationSpec JavaBeans
     * @throws ResourceException generic exception
     * @return an array of XAResource objects
     */
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        log.debug("call getXAResources");
        return null;
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx a bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log.debug("call start");

    }

    /**
     * This is called when a resource adapter instance is undeployed or during application server shutdown.
     */
    public void stop() {
        log.debug("call stop");

    }

    /**
     * Hash code
     *
     * @return The hash
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Equals
     *
     * @param other The other object
     * @return True if equal; otherwise false
     */
    public boolean equals(Object other) {
        return super.equals(other);
    }
}
