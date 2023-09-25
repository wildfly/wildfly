/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import org.jboss.logging.Logger;
import jakarta.resource.spi.Activation;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.validation.constraints.NotNull;

/**
 * AnnoActivationSpec
 *
 * @version $Revision: $
 */
@Activation(messageListeners = {AnnoMessageListener.class,
        AnnoMessageListener1.class})
public class AnnoActivationSpec implements ActivationSpec {

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("AnnoActivationSpec");

    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    /**
     * first
     */
    @ConfigProperty(defaultValue = "C", description = {"1st", "first"}, ignore = true, supportsDynamicUpdates = false, confidential = true)
    @NotNull
    private Character first;

    /**
     * second
     */
    private Double second;

    /**
     * Default constructor
     */
    public AnnoActivationSpec() {

    }

    /**
     * Set first
     *
     * @param first The value
     */
    public void setFirst(Character first) {
        this.first = first;
    }

    /**
     * Get first
     *
     * @return The value
     */
    public Character getFirst() {
        return first;
    }

    /**
     * Set second
     *
     * @param second The value
     */
    @ConfigProperty(defaultValue = "0.5", description = {"2nd", "second"}, ignore = false, supportsDynamicUpdates = true, confidential = false)
    public void setSecond(Double second) {
        this.second = second;
    }

    /**
     * Get second
     *
     * @return The value
     */
    public Double getSecond() {
        return second;
    }

    /**
     * This method may be called by a deployment tool to validate the overall
     * activation configuration information provided by the endpoint deployer.
     *
     * @throws InvalidPropertyException indicates invalid configuration property settings.
     */
    public void validate() throws InvalidPropertyException {
        log.trace("validate()");
    }

    /**
     * Get the resource adapter
     *
     * @return The handle
     */
    public ResourceAdapter getResourceAdapter() {
        log.trace("getResourceAdapter()");
        return ra;
    }

    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */
    public void setResourceAdapter(ResourceAdapter ra) {
        log.trace("setResourceAdapter()");
        this.ra = ra;
    }

}
