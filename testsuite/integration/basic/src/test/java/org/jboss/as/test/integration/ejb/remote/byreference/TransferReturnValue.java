/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byreference;

/**
 * A return value class that does not implement serializable
 */
public class TransferReturnValue {

    private String value;

    public TransferReturnValue() {
    }

    public TransferReturnValue( String value ) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public void setValue ( String value ) {
        this.value = value;
    }

    public String getString() {
        return value;
    }
}
