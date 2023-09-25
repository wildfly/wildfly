/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;
import java.io.Serializable;

/**
 * @author Jaikiran Pai
 */
public class SimpleActivationSpec implements ActivationSpec, Serializable {

    private ResourceAdapter ra;
    private String someProp;

    public ResourceAdapter getResourceAdapter() {
        return this.ra;
    }

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.ra = ra;
    }

    public void validate() throws InvalidPropertyException {
    }

    public String getSomeProp() {
        return this.someProp;
    }

    public void setSomeProp(String prop) {
        this.someProp = prop;
    }
}

