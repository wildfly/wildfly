/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.service;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;

/**
 * A service for getting a property to be stored in endpoint / client config.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class PropertyService implements Service {

    private final String propName;
    private final String propValue;
    private final Consumer<PropertyService> propertyServiceConsumer;

    public PropertyService(final String propName, final String propValue, final Consumer<PropertyService> propertyServiceConsumer) {
        this.propValue = propValue;
        this.propName = propName;
        this.propertyServiceConsumer = propertyServiceConsumer;
    }

    public String getPropName() {
        return propName;
    }

    public String getPropValue() {
        return propValue;
    }

    @Override
    public void start(final StartContext context) {
        propertyServiceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        propertyServiceConsumer.accept(null);
    }

}
