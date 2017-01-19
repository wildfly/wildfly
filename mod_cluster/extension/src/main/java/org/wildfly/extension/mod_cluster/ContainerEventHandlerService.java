/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
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
package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import org.jboss.modcluster.ModClusterService;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * Service starting mod_cluster service.
 *
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
public class ContainerEventHandlerService implements Service<ModClusterService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(ModClusterExtension.SUBSYSTEM_NAME);
    public static final ServiceName CONFIG_SERVICE_NAME = SERVICE_NAME.append("config");

    private LoadBalanceFactorProvider load;
    private Value<ModClusterConfiguration> configurationValue;
    private volatile ModClusterService eventHandler;

    ContainerEventHandlerService(Value<ModClusterConfiguration> configurationValue, LoadBalanceFactorProvider factorProvider) {
        this.configurationValue = configurationValue;
        this.load = factorProvider;
    }

    @Override
    public ModClusterService getValue() throws IllegalStateException, IllegalArgumentException {
        return this.eventHandler;
    }

    @Override
    public void start(StartContext context) {
        ROOT_LOGGER.debugf("Starting mod_cluster extension");

        this.eventHandler = new ModClusterService(configurationValue.getValue(), load);
    }

    @Override
    public void stop(StopContext context) {
        this.eventHandler.shutdown();
        this.eventHandler = null;
    }
}
