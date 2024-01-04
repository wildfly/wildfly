/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

import org.jboss.logging.Logger;

/**
 * BaseActivationSpec
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
public class BaseActivationSpec implements ActivationSpec {

    private static Logger log = Logger.getLogger(BaseActivationSpec.class);

    /**
     * This method may be called by a deployment tool to validate the overall activation configuration information provided by
     * the endpoint deployer.
     *
     * @throws InvalidPropertyException indicates invalid configuration property settings.
     */
    @SuppressWarnings(value = { "deprecation" })
    public void validate() throws InvalidPropertyException {
        log.debug("call validate");

    }

    /**
     * Get the associated <code>ResourceAdapter</code> object.
     *
     * @return the associated <code>ResourceAdapter</code> object.
     */
    public ResourceAdapter getResourceAdapter() {
        log.debug("call getResourceAdapter");
        return null;
    }

    /**
     * Associate this object with a <code>ResourceAdapter</code> object.
     *
     * @param ra <code>ResourceAdapter</code> object to be associated with.
     *
     * @throws ResourceException generic exception.
     */
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        log.debug("call setResourceAdapter");

    }

}
