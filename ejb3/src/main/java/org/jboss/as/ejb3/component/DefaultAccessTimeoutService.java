/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
