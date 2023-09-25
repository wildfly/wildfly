/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.messagedriven;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * User: jpai
 */
public class DefaultResourceAdapterService implements Service<DefaultResourceAdapterService> {

    public static final ServiceName DEFAULT_RA_NAME_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("default-resource-adapter-name-service");

    private volatile String defaultResourceAdapterName;

    public DefaultResourceAdapterService(final String resourceAdapterName) {
        this.defaultResourceAdapterName = resourceAdapterName;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DefaultResourceAdapterService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public synchronized void setResourceAdapterName(final String resourceAdapterName) {
        this.defaultResourceAdapterName = resourceAdapterName;
    }

    public synchronized String getDefaultResourceAdapterName() {
        return this.defaultResourceAdapterName;
    }

}
