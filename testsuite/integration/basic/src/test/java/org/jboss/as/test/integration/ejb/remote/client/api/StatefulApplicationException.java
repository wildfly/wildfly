/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import jakarta.ejb.ApplicationException;
import java.io.Serializable;

/**
 * User: jpai
 */
@ApplicationException
public class StatefulApplicationException extends Exception implements Serializable {

    private final String state;

    public StatefulApplicationException(final String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }
}
