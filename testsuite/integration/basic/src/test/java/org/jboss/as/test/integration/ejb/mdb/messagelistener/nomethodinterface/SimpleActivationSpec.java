/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagelistener.nomethodinterface;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

/**
 * @author Jan Martiska
 */
public class SimpleActivationSpec implements ActivationSpec {

    private ResourceAdapter ra;

    // The message listener method. The RA will access this method via reflection.
    private String methodName;

    @Override
    public void validate() throws InvalidPropertyException {
        if (methodName == null) {
            throw new InvalidPropertyException("methodName property needs to be specified ");
        }
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return this.ra;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
        this.ra = resourceAdapter;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
