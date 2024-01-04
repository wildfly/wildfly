/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.impl;


class TelnetOption {

    protected int optionCode;

    protected boolean supported;

    protected boolean enabled;

    protected boolean negotiated;

    protected boolean inNegotiation;

    TelnetOption(int optionCode) {
        this.optionCode = optionCode;
    }

    public int getOptionId() {
        return optionCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        enabled = true;
        negotiated = true;
    }

    public void disable() {
        enabled = false;
        negotiated = true;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean hasBeenNegotiated() {
        return negotiated;
    }

    public boolean isInNegotiation() {
        return inNegotiation;
    }

    public void hasBeenNegotiated(boolean negotiated) {
        this.negotiated = negotiated;
        this.inNegotiation = false;
    }
}