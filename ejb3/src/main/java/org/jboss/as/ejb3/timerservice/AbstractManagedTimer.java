/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.Map;
import java.util.Objects;

import org.jboss.as.ejb3.timerservice.spi.ManagedTimer;

/**
 * Abstract managed timer with common {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()} implementations.
 * @author Paul Ferraro
 */
public abstract class AbstractManagedTimer implements ManagedTimer {

    private final String component;
    private final String id;

    protected AbstractManagedTimer(String component, String id) {
        this.component = component;
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.component, this.id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AbstractManagedTimer timer)) return false;
        return this.component.equals(timer.component) && this.id.equals(timer.id);
    }

    @Override
    public String toString() {
        return Map.of("component", this.component, "id", this.id).toString();
    }
}
