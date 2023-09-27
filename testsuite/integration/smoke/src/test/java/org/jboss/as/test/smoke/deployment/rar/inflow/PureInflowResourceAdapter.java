/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.inflow;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * PureInflowResourceAdapter
 *
 * @version $Revision: $
 */
public class PureInflowResourceAdapter implements ResourceAdapter {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger(PureInflowResourceAdapter.class);

    /**
     * The activations by activation spec
     */
    private Map<PureInflowActivationSpec, PureInflowActivation> activations;

    /**
     * Default constructor
     */
    public PureInflowResourceAdapter() {
        this.activations = Collections.synchronizedMap(new HashMap<PureInflowActivationSpec, PureInflowActivation>());
    }

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory,
                                   ActivationSpec spec) throws ResourceException {
        PureInflowActivation activation = new PureInflowActivation(this, endpointFactory, (PureInflowActivationSpec) spec);
        activations.put((PureInflowActivationSpec) spec, activation);
        activation.start();

        log.trace("endpointActivation()");
    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory,
                                     ActivationSpec spec) {
        PureInflowActivation activation = (PureInflowActivation) activations.remove(spec);
        if (activation != null) { activation.stop(); }

        log.trace("endpointDeactivation()");
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx A bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx)
            throws ResourceAdapterInternalException {
        log.trace("start()");
    }

    /**
     * This is called when a resource adapter instance is undeployed or
     * during application server shutdown.
     */
    public void stop() {
        log.trace("stop()");
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs An array of ActivationSpec JavaBeans
     * @return An array of XAResource objects
     * @throws ResourceException generic exception
     */
    public XAResource[] getXAResources(ActivationSpec[] specs)
            throws ResourceException {
        log.trace("getXAResources()");
        return null;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = 17;
        return result;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }

        if (other == this) { return true; }

        if (!(other instanceof PureInflowResourceAdapter)) { return false; }

        PureInflowResourceAdapter obj = (PureInflowResourceAdapter) other;
        boolean result = true;
        return result;
    }
}
