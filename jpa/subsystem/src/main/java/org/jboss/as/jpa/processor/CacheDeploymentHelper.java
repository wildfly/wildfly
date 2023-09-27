/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import org.jboss.as.jpa.processor.secondlevelcache.CacheDeploymentListener;
import org.jipijapa.event.impl.EventListenerRegistration;

/**
 * CacheDeploymentHelper
 *
 * @author Scott Marlow
 */
public class CacheDeploymentHelper {

    private volatile CacheDeploymentListener listener;

    public void register() {
        listener = new CacheDeploymentListener();
        EventListenerRegistration.add(listener);
    }

    public void unregister() {
        if (listener != null) {
            EventListenerRegistration.remove(listener);
            listener = null;
        }
    }
}
