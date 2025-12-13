/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import java.util.Map;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * Remote stateless bean for tracking status of passivation/activation events.
 *
 * @author Radoslav Husar
 */
@Stateless
@Remote(PassivationEventTracker.class)
public class PassivationEventTrackerBean implements PassivationEventTracker {

    @Override
    public void clearPassivationEvents() {
        PassivationEventTrackerUtil.clearEvents();
    }

    @Override
    public Map.Entry<Object, PassivationEventTrackerUtil.EventType> pollPassivationEvent() {
        return PassivationEventTrackerUtil.pollEvent();
    }
}
