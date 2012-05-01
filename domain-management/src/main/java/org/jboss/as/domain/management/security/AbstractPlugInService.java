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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.ModelDescriptionConstants.NAME;
import static org.jboss.as.domain.management.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.domain.management.ModelDescriptionConstants.VALUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Base service to be extended by the plug-in services.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class AbstractPlugInService {

    private final InjectedValue<PlugInLoaderService> plugInLoader = new InjectedValue<PlugInLoaderService>();
    private final ModelNode model;
    private String name;
    private Map<String, String> configurationProperties;

    AbstractPlugInService(ModelNode model) {
        this.model = model;
    }

    public InjectedValue<PlugInLoaderService> getPlugInLoaderServiceValue() {
        return plugInLoader;
    }

    public void start(final StartContext context) throws StartException {
        name = model.require(NAME).asString();
        if (model.hasDefined(PROPERTY)) {
            List<Property> propertyList = model.require(PROPERTY).asPropertyList();
            Map<String, String> configurationProperties = new HashMap<String, String>(propertyList.size());

            for (Property current : propertyList) {
                String propertyName = current.getName();
                String value = null;
                if (current.getValue().hasDefined(VALUE)) {
                    value = current.getValue().require(VALUE).asString();
                }
                configurationProperties.put(propertyName, value);
            }
            this.configurationProperties = Collections.unmodifiableMap(configurationProperties);
        } else {
            configurationProperties = Collections.emptyMap();
        }
    }

    public void stop(final StopContext context) {
        name = null;
        configurationProperties = null;
    }

    protected String getPlugInName() {
        return name;
    }

    protected Map<String, String> getConfiguration() {
        return configurationProperties;
    }

    protected PlugInLoaderService getPlugInLoader() {
        return plugInLoader.getValue();
    }

}
