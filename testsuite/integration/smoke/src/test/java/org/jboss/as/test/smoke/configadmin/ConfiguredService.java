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

package org.jboss.as.test.smoke.configadmin;

import org.jboss.as.configadmin.service.ConfigAdminService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;

import java.util.Dictionary;

/**
 * A simple service that reads its configuration from the {@link ConfigAdminService}
 *
 * @author Thomas.Diesler@jboss.org
 * @since 12-Dec-2010
 */
public class ConfiguredService extends AbstractService<ConfiguredService> {

    public static ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "smoke", "configadmin");
    public static String SERVICE_PID = "org.jboss.as.test.embedded.configadmin";

    private final InjectedValue<ConfigAdminService> injectedConfigAdmin = new InjectedValue<ConfigAdminService>();
    private Dictionary<String, String> config;

    public static void addService(ServiceTarget serviceTarget) {
        ConfiguredService service = new ConfiguredService();
        ServiceBuilder<ConfiguredService> serviceBuilder = serviceTarget.addService(SERVICE_NAME, service);
        serviceBuilder.addDependency(ConfigAdminService.SERVICE_NAME, ConfigAdminService.class, service.injectedConfigAdmin);
        serviceBuilder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        config = injectedConfigAdmin.getValue().getConfiguration(SERVICE_PID);
    }

    public String getConfigValue(String key) {
        return config != null ? config.get(key) : null;
    }

    @Override
    public ConfiguredService getValue() throws IllegalStateException {
        return this;
    }
}
