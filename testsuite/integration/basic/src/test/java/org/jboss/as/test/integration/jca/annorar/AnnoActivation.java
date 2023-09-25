/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * AnnoActivation
 *
 * @version $Revision: $
 */
public class AnnoActivation {

    /**
     * The resource adapter
     */
    private AnnoResourceAdapter ra;

    /**
     * Activation spec
     */
    private AnnoActivationSpec spec;

    /**
     * The message endpoint factory
     */
    private MessageEndpointFactory endpointFactory;

    /**
     * Default constructor
     *
     * @throws ResourceException Thrown if an error occurs
     */
    public AnnoActivation() throws ResourceException {
        this(null, null, null);
    }

    /**
     * Constructor
     *
     * @param ra              AnnoResourceAdapter
     * @param endpointFactory MessageEndpointFactory
     * @param spec            AnnoActivationSpec
     * @throws ResourceException Thrown if an error occurs
     */
    public AnnoActivation(AnnoResourceAdapter ra,
                          MessageEndpointFactory endpointFactory, AnnoActivationSpec spec)
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
    public AnnoActivationSpec getActivationSpec() {
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
