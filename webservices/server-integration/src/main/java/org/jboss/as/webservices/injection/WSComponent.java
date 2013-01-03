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

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSComponent extends BasicComponent {

    private volatile BasicComponentInstance wsComponentInstance;

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
        if (wsComponentInstance == null) {
            synchronized (lock) {
                if (wsComponentInstance == null) {
                    wsComponentInstance = (BasicComponentInstance) createInstance();
                }
            }
        }
        return wsComponentInstance;
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
