/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.msc.inject.ConcurrentMapInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class DataSourceConfigService implements Service<ModifiableDataSource> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("data-source-config");

    private final ConcurrentMap<String, String> connectionProperties = new ConcurrentHashMap<String, String>(0);


    private final ModifiableDataSource dataSourceConfig;

    public DataSourceConfigService(ModifiableDataSource dataSourceConfig) {
        super();
        this.dataSourceConfig = dataSourceConfig;
    }

    public synchronized void start(StartContext startContext) throws StartException {
        for (Map.Entry<String, String> connectionProperty : connectionProperties.entrySet()) {
            dataSourceConfig.addConnectionProperty(connectionProperty.getKey(), connectionProperty.getValue());
        }
    }

    public synchronized void stop(StopContext stopContext) {
    }

    @Override
    public ModifiableDataSource getValue() throws IllegalStateException, IllegalArgumentException {
        return dataSourceConfig;
    }

    public Injector<String> getConnectionPropertyInjector(String key) {
        return new ConcurrentMapInjector(connectionProperties, key);
    }

}
