/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.service;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.osgi.parser.OSGiSubsystemState;
import org.jboss.as.osgi.parser.OSGiSubsystemState.Activation;
import org.jboss.as.osgi.parser.OSGiSubsystemState.OSGiModule;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.osgi.framework.Constants;

/**
 * The configuration for the OSGi service.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Sep-2010
 */
public class Configuration implements Service<Configuration> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("osgi", "configuration");

    /** The property that defines a comma seperated list of system module identifiers */
    static final String PROP_JBOSS_OSGI_SYSTEM_MODULES = "org.jboss.osgi.system.modules";

    private InjectedValue<ServerEnvironment> injectedEnvironment = new InjectedValue<ServerEnvironment>();
    private final OSGiSubsystemState subsystemState;

    public static void addService(final BatchBuilder batchBuilder, final OSGiSubsystemState state) {
        Configuration config = new Configuration(state);
        batchBuilder.addService(SERVICE_NAME, config)
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, config.injectedEnvironment)
            .setInitialMode(Mode.ACTIVE)
            .install();
    }

    public static Configuration getServiceValue(ServiceContainer container) {
        try {
            ServiceController<?> controller = container.getRequiredService(SERVICE_NAME);
            return (Configuration) controller.getValue();
        } catch (ServiceNotFoundException ex) {
            throw new IllegalStateException("Cannot obtain required service: " + SERVICE_NAME);
        }
    }

    private Configuration(final OSGiSubsystemState state) {
        this.subsystemState = state;
    }

    public Activation getActivationPolicy() {
        return subsystemState.getActivationPolicy();
    }

    public Map<String, Object> getProperties() {

        Map<String, Object> properties = new LinkedHashMap<String, Object>(subsystemState.getProperties());
        String storage = (String) properties.get(Constants.FRAMEWORK_STORAGE);
        if (storage == null) {
            ServerEnvironment envirionment = injectedEnvironment.getValue();
            File dataDir = envirionment.getServerDataDir();
            storage = dataDir.getAbsolutePath() + File.separator + "osgi-store";
            properties.put(Constants.FRAMEWORK_STORAGE, storage);
        }

        return Collections.unmodifiableMap(properties);
    }

    public List<OSGiModule> getModules() {
        return subsystemState.getModules();
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public Configuration getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public String toString() {
        return "OSGiServiceConfiguration: " + subsystemState;
    }
}
