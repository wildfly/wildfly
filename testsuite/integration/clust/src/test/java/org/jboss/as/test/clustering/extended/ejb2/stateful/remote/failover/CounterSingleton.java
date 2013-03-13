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

package org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.log4j.Logger;

@Singleton
@Startup
public class CounterSingleton implements CounterSingletonRemote {
    public static AtomicInteger destroyCounter = new AtomicInteger(0);
    private static final Logger log = Logger.getLogger(CounterSingleton.class);

    public int getDestroyCount() {
        log.info("destroyCounter: " + destroyCounter.get());
        return destroyCounter.get();
    }

    public void resetDestroyCount() {
        destroyCounter.set(0);
    }
}
