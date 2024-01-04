/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.inflow;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.logging.Logger;

/**
 * PureInflowActivationSpec
 *
 * @version $Revision: $
 */
public class PureInflowActivationSpec implements ActivationSpec {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger(PureInflowActivationSpec.class);

    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    /**
     * Default constructor
     */
    public PureInflowActivationSpec() {

    }

    /**
     * This method may be called by a deployment tool to validate the overall
     * activation configuration information provided by the endpoint deployer.
     *
     * @throws InvalidPropertyException indicates invalid onfiguration property settings.
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
