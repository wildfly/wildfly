/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transaction.inflow;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

/**
 * Mock spec rar class. Referred in ra.xml.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowRaSpec implements ActivationSpec {

    private volatile ResourceAdapter resourceAdapter;
    private volatile String action;

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        this.resourceAdapter = ra;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        // everything ok
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return String.format("%s <rar = %s, action = %s> [%s]",
            TransactionInflowRaSpec.class.getName(), resourceAdapter, action, super.toString());
    }
}
