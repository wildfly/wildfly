/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component;

import org.jboss.as.ejb3.concurrency.AccessTimeoutDetails;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.concurrent.TimeUnit;

/**
 * Service that manages the default access timeout for session beans
 *
 * @author Stuart Douglas
 */
public class DefaultAccessTimeoutService implements Service<DefaultAccessTimeoutService> {


    public static final ServiceName STATEFUL_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "statefulDefaultTimeout");
    public static final ServiceName SINGLETON_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "singletonDefaultTimeout");


    private volatile AccessTimeoutDetails value;

    public DefaultAccessTimeoutService(final long value) {
        this.value = new AccessTimeoutDetails(value, TimeUnit.MILLISECONDS);
    }

    public final AccessTimeoutDetails getDefaultAccessTimeout() {
        return value;
    }

    public void setDefaultAccessTimeout(final long value) {
        this.value = new AccessTimeoutDetails(value, TimeUnit.MILLISECONDS);
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public DefaultAccessTimeoutService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
