/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.injection;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.naming.ManagedReference;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class WSComponent extends BasicComponent {

    private volatile BasicComponentInstance wsComponentInstance;
    private volatile ManagedReference reference;

    /**
     * We can't lock on <code>this</code> because the
     * {@link org.jboss.as.ee.component.BasicComponent#waitForComponentStart()}
     * also synchronizes on it, and calls {@link #wait()}.
     */
    private final Object lock = new Object();

    public WSComponent(final WSComponentCreateService createService) {
        super(createService);
    }

    public BasicComponentInstance getComponentInstance() {
        BasicComponentInstance result = wsComponentInstance;
        if (result == null) {
            synchronized (lock) {
                result = wsComponentInstance;
                if (result == null) {
                    if (reference == null) {
                        wsComponentInstance = result = (BasicComponentInstance) createInstance();
                    } else {
                        wsComponentInstance = result = (BasicComponentInstance) this.createInstance(reference.getInstance());
                    }
                }
            }
        }
        return result;
     }

    public void setReference(ManagedReference reference) {
        this.reference = reference;
    }

    @Override
    public void stop() {
        if (wsComponentInstance == null) return;
        synchronized(lock) {
            if (wsComponentInstance != null) {
                wsComponentInstance.destroy();
                wsComponentInstance = null;
            }
        }
    }
}

