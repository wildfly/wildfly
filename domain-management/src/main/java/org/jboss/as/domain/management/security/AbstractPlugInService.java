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

import java.util.Map;

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
    private final String realmName;
    private final String pluginName;
    private final Map<String, String> configurationProperties;

    AbstractPlugInService(final String realmName, final String pluginName, final Map<String, String> properties) {
        this.realmName = realmName;
        this.pluginName = pluginName;
        this.configurationProperties = properties;
    }

    public InjectedValue<PlugInLoaderService> getPlugInLoaderServiceValue() {
        return plugInLoader;
    }

    public void start(final StartContext context) throws StartException {
    }

    public void stop(final StopContext context) {
    }

    protected String getRealmName() {
        return realmName;
    }

    protected String getPlugInName() {
        return pluginName;
    }

    protected Map<String, String> getConfiguration() {
        return configurationProperties;
    }

    protected PlugInLoaderService getPlugInLoader() {
        return plugInLoader.getValue();
    }

}
