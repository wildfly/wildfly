/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer.passivation.bean;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.jboss.as.test.clustering.PassivationEventTrackerUtil;

/**
 * Serializable timer info object used to test timer passivation.
 * This object is stored with the timer and captures passivation/activation cycles.
 * Extends {@link PassivationEventTrackerUtil} to track serialization/deserialization events
 * in a static queue that can be polled without triggering timer activation.
 *
 * @author Radoslav Husar
 */
public class TimerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3314782443771710521L;

    private final String name;

    public TimerInfo(String name) {
        this.name = name;
        PassivationEventTrackerUtil.clearEvents();
    }

    public String getName() {
        return this.name;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Record passivation event
        PassivationEventTrackerUtil.recordPassivation(this.name);

        System.out.println("TimerInfo.writeObject() called for: " + this.name);

        // Perform default serialization
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Perform default deserialization
        in.defaultReadObject();

        // Record activation event
        PassivationEventTrackerUtil.recordActivation(this.name);

        System.out.println("TimerInfo.readObject() called for: " + this.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TimerInfo)) return false;
        TimerInfo other = (TimerInfo) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        return String.format("TimerInfo{name=%s}", this.name);
    }
}
