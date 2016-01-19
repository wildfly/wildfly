/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

