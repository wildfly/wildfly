/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * Activation
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ValidActivation {
    /**
     * The resource adapter
     */
    private ValidResourceAdapter ra;

    /**
     * Activation spec
     */
    private ValidActivationSpec spec;

    /**
     * The message endpoint factory
     */
    private MessageEndpointFactory endpointFactory;

    /**
     * Default constructor
     *
     * @throws ResourceException Thrown if an error occurs
     */
    public ValidActivation() throws ResourceException {
        this(null, null, null);
    }

    /**
     * Constructor
     *
     * @param ra              ResourceAdapter
     * @param endpointFactory MessageEndpointFactory
     * @param spec            ActivationSpec
     * @throws ResourceException Thrown if an error occurs
     */
    public ValidActivation(ValidResourceAdapter ra, MessageEndpointFactory endpointFactory, ValidActivationSpec spec)
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
    public ValidActivationSpec getActivationSpec() {
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
