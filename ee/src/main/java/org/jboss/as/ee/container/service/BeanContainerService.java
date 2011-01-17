/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.container.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.as.ee.container.BeanContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for a bean container.
 *
 * @author John Bailey
 */
public class BeanContainerService implements Service<BeanContainer<?>> {

    private final AtomicBoolean started = new AtomicBoolean();

    private final BeanContainer<?> beanContainer;

    public BeanContainerService(final BeanContainer<?> beanContainer) {
        this.beanContainer = beanContainer;
    }

    public void start(StartContext context) throws StartException {
        if(!started.compareAndSet(false, true)) {
            throw new StartException("Unable to start bean container.  Already started.");
        }
        beanContainer.start();
    }

    public void stop(StopContext context) {
        if(started.compareAndSet(true, false)) {
            beanContainer.stop();
        } else {
            throw new IllegalStateException("Unable to stop bean container.  Already stopped.");
        }
    }

    public BeanContainer<?> getValue() throws IllegalStateException, IllegalArgumentException {
        if(!started.get() ) {
            throw new IllegalStateException("Unable to retrieve bean container.  Service is stopped.");
        }
        return beanContainer;
    }


}
