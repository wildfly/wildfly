/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.event.impl;

import org.jipijapa.event.impl.internal.Notification;
import org.jipijapa.event.spi.EventListener;

/**
 * System level EventListenerRegistration
 *
 * @author Scott Marlow
 */
public class EventListenerRegistration {

    public static void add(EventListener eventListener) {
        Notification.add(eventListener);
    }

    public static void remove(EventListener eventListener) {
        Notification.remove(eventListener);
    }


}
