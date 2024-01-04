/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.inflow;


import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * PureInflowActivation
 */
public class PureInflowActivation {
    /**
     * The resource adapter
     */
    private PureInflowResourceAdapter ra;

    /**
     * Activation spec
     */
    private PureInflowActivationSpec spec;

    /**
     * The message endpoint factory
     */
    private MessageEndpointFactory endpointFactory;

    /**
     * Default constructor
     *
     * @throws ResourceException Thrown if an error occurs
     */
    public PureInflowActivation() throws ResourceException {
        this(null, null, null);
    }

    /**
     * Constructor
     *
     * @param ra              PureInflowResourceAdapter
     * @param endpointFactory MessageEndpointFactory
     * @param spec            PureInflowActivationSpec
     * @throws ResourceException Thrown if an error occurs
     */
    public PureInflowActivation(PureInflowResourceAdapter ra,
                                MessageEndpointFactory endpointFactory,
                                PureInflowActivationSpec spec)
            throws ResourceException {
        this.ra = ra;
        this.endpointFactory = endpointFactory;
        this.spec = spec;
    }

    /**
     * Get activation spec class
     *
     * @return Activation spec
     */
    public PureInflowActivationSpec getActivationSpec() {
        return spec;
    }

    /**
     * Get message endpoint factory
     *
     * @return Message endpoint factory
     */
    public MessageEndpointFactory getMessageEndpointFactory() {
        return endpointFactory;
    }

    /**
     * Start the activation
     *
     * @throws ResourceException Thrown if an error occurs
     */
    public void start() throws ResourceException {

    }

    /**
     * Stop the activation
     */
    public void stop() {

    }
}
