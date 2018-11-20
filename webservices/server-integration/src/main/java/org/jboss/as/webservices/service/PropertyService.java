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
