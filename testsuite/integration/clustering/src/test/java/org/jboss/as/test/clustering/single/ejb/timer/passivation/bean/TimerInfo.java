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
import java.time.Instant;

/**
 * Serializable timer info object used to test timer passivation.
 * This object is stored with the timer and captures passivation/activation cycles.
 *
 * @author Radoslav Husar
 */
public class TimerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3314782443771710521L;

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Record passivation prior to serialisation
        PassivationTimeoutCollectorBean.PASSIVATIONS.add(Instant.now());
        System.out.println("TimerInfo.writeObject() called for: " + this.getClass().getName());

        // Perform default serialization
        out.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Perform default deserialization
        in.defaultReadObject();

        // Record passivation following to deserialisation
        PassivationTimeoutCollectorBean.ACTIVATIONS.add(Instant.now());

        System.out.println("TimerInfo.readObject() called for: " + this.getClass().getName());
    }
}
