/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.util.concurrent.ScheduledExecutorService;

import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.Time;

/**
 * The expiration-related configuration of a bean manager.
 *
 * @author Paul Ferraro
 */
public interface ExpirationConfiguration<T> {
    /**
     * The duration of time a bean can be idle after which it will expire.
     * @return a time duration
     */
    Time getTimeout();

    /**
     * Returns the listener to notify of bean removal/expiration.
     * @return an event listener.
     */
    RemoveListener<T> getRemoveListener();

    /**
     * Returns the scheduled executor suitable for scheduling bean expiration.
     * @return a scheduled executor
     */
    ScheduledExecutorService getExecutor();
}
