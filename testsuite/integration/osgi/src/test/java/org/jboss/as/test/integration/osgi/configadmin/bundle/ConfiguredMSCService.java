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

package org.jboss.as.test.integration.osgi.configadmin.bundle;

import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.as.configadmin.ConfigAdminListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Set;

/**
 * A simple service that reads its configuration from the {@link ConfigAdmin}
 *
 * @author Thomas.Diesler@jboss.org
 * @since 12-Dec-2010
 */
public class ConfiguredMSCService implements Service<ConfiguredMSCService> {

    public static String SERVICE_PID = ConfiguredMSCService.class.getName();

    private final InjectedValue<ConfigAdmin> injectedConfigAdmin = new InjectedValue<ConfigAdmin>();
    private Dictionary<String, String> config;
    private ConfigAdminListener listener;

    public static ServiceController<ConfiguredMSCService> addService(ServiceTarget serviceTarget, ServiceListener<ConfiguredMSCService> listener) {
        ConfiguredMSCService service = new ConfiguredMSCService();
        ServiceName serviceName = ServiceName.parse(ConfiguredMSCService.class.getName());
        ServiceBuilder<ConfiguredMSCService> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(ConfigAdmin.SERVICE_NAME, ConfigAdmin.class, service.injectedConfigAdmin);
        builder.addListener(listener);
        return builder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ConfigAdmin configAdmin = injectedConfigAdmin.getValue();
        ConfigAdminListener listener = new ConfigAdminListener() {

            @Override
            public void configurationModified(String pid, Dictionary<String, String> props) {
                config = props;
            }

            @Override
            public Set<String> getPIDs() {
                return Collections.singleton(SERVICE_PID);
            }
        };
        configAdmin.addListener(listener);
    }

    @Override
    public void stop(StopContext context) {
        ConfigAdmin configAdmin = injectedConfigAdmin.getValue();
        configAdmin.removeListener(listener);
    }

    @Override
    public ConfiguredMSCService getValue() throws IllegalStateException {
        return this;
    }

    public Dictionary<String, String> getConfig() {
        return config;
    }
}
