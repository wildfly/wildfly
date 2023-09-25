/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byreference;

/**
 * A class that does not implement serializable used in pass-by-reference
 */
public class TransferParameter {

    private String value;

    public TransferParameter() {
    }

    public TransferParameter( String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public void setValue ( String value ) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
}

