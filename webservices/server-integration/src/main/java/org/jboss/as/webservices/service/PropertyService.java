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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.metadata.config.AbstractCommonConfig;

/**
 * A service for setting a property into an endpoint / client config.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class PropertyService<T extends AbstractCommonConfig> implements Service<String> {

    private InjectedValue<T> abstractCommonConfig = new InjectedValue<T>();
    private final String propValue;
    private final String propName;

    public PropertyService(String propName, String propValue) {
        this.propValue = propValue;
        this.propName = propName;
    }

    @Override
    public String getValue() {
        return propValue;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final AbstractCommonConfig commonConfig = abstractCommonConfig.getValue();
        synchronized (commonConfig) {  //JBWS-3707
            commonConfig.setProperty(propName, propValue);
        }
    }

    @Override
    public void stop(final StopContext context) {
        final AbstractCommonConfig commonConfig = abstractCommonConfig.getValue();
        synchronized (commonConfig) {  //JBWS-3707
            if (commonConfig.getProperties().containsKey(propName)) {
                commonConfig.getProperties().remove(propName);
            }
        }
    }

    public InjectedValue<T> getAbstractCommonConfig() {
        return abstractCommonConfig;
    }
}
