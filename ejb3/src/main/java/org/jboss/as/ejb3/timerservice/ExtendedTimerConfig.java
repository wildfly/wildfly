/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice;

import jakarta.ejb.TimerConfig;

import java.io.Serializable;

/**
 * WildFly specific extension of {@link TimerConfig} that allows for an external identifier
 * to be associated with an EJB timer.
 * <p>
 * This identifier is stored in a dedicated database column to allow for high-performance
 * indexed lookups without deserializing the timer info object.
 * </p>
 */
public class ExtendedTimerConfig extends TimerConfig {

    private String externalId;

    public ExtendedTimerConfig() {
        super();
    }

    public ExtendedTimerConfig(Serializable info, boolean persistent) {
        super(info, persistent);
    }

    /**
     * Creates a timer configuration with an external identifier.
     *
     * @param info       application specific information
     * @param persistent true if the timer should be persistent
     * @param externalId the external identifier for indexed lookups
     */
    public ExtendedTimerConfig(Serializable info, boolean persistent, String externalId) {
        super(info, persistent);
        this.externalId = externalId;
    }

    /**
     * Returns the external identifier associated with this timer configuration.
     *
     * @return the external identifier, or null if not set
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Sets the external identifier associated with this timer configuration.
     *
     * @param externalId the external identifier
     */
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}