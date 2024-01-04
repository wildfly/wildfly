/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.activationname.adapter;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

/**
 * @author Ivo Studensky
 */
public class SimpleActivationSpec implements ActivationSpec {

    private ResourceAdapter resourceAdapter;

    public SimpleActivationSpec() {
    }

    @Override
    public void validate() throws InvalidPropertyException {
        // nothing to validate here
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.resourceAdapter = ra;
    }
}
