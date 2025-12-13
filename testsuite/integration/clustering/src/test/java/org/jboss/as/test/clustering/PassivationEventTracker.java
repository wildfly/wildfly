/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import java.util.Map;

/**
 * Interface for tracking status of passivation/activation events.
 *
 * @author Radoslav Husar
 */
public interface PassivationEventTracker {

    /**
     * Clears all passivation events from the event queue.
     */
    void clearPassivationEvents();

    /**
     * Polls for the next passivation event from the event queue.
     * Returns the event key (session/bean identifier) and type (PASSIVATION/ACTIVATION).
     *
     * @return map entry of identifier to EventType, or null if no event is available
     */
    Map.Entry<Object, PassivationEventTrackerUtil.EventType> pollPassivationEvent();
}
