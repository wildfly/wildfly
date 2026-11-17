/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import java.io.Serializable;

/**
 * Mock dangerous class for testing IIOP deserialization deny-list filter.
 * This class simulates a dangerous gadget class that should be blocked.
 */
public class DangerousClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private String payload;

    public DangerousClass() {
    }

    public DangerousClass(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
