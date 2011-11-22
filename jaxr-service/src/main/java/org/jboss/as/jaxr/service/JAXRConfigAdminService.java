/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.service;

import org.jboss.as.jaxr.extension.JAXRConstants;
import org.jboss.as.jaxr.extension.JAXRWriteAttributeHandler;
import org.jboss.as.osgi.service.ConfigAdminListener;
import org.jboss.as.osgi.service.ConfigAdminService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Set;

import static org.jboss.as.jaxr.extension.JAXRWriteAttributeHandler.applyUpdateToConfig;

/**
 * The configuration service of the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Nov-2011
 */
public class JAXRConfigAdminService extends AbstractService<JAXRConfiguration> {

    public static final ServiceName SERVICE_NAME = JAXRConstants.SERVICE_BASE_NAME.append("configuration");

    // [TODO] AS7-2277 JAXR subsystem i18n
    private final Logger log = Logger.getLogger(JAXRConfigAdminService.class);

    private final InjectedValue<ConfigAdminService> injectedConfigAdmin = new InjectedValue<ConfigAdminService>();
    private final JAXRConfiguration config = new JAXRConfiguration();

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        JAXRConfigAdminService service = new JAXRConfigAdminService();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ConfigAdminService.SERVICE_NAME, ConfigAdminService.class, service.injectedConfigAdmin);
        builder.addListener(listeners);
        return builder.install();
    }

    // Hide ctor
    private JAXRConfigAdminService() {
    }

    @Override
    public void start(StartContext context) throws StartException {
        ConfigAdminService configAdmin = injectedConfigAdmin.getValue();
        configAdmin.addListener(new ConfigAdminListener() {
            @Override
            public void configurationModified(String pid, Dictionary<String, String> props) {
                synchronized (config) {
                    config.init();
                    log.infof("JAXR Configuration: %s", props);
                    if (props != null) {
                        for (String attr : JAXRWriteAttributeHandler.REQUIRED_ATTRIBUTES) {
                            String value = props.get(attr);
                            applyUpdateToConfig(config, attr, value);
                        }
                    }
                }
            }

            @Override
            public Set<String> getPIDs() {
                return Collections.singleton(JAXRConfiguration.class.getName());
            }
        });
    }

    @Override
    public JAXRConfiguration getValue() throws IllegalStateException {
        synchronized (config) {
            return config;
        }
    }
}
