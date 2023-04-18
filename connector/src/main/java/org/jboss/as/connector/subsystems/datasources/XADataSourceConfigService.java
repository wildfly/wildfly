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
import java.util.function.Supplier;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class XADataSourceConfigService implements Service<ModifiableXaDataSource> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("xa-data-source-config");

    private final ModifiableXaDataSource dataSourceConfig;

    private final Map<String, Supplier<String>> xaDataSourceProperties;

    public XADataSourceConfigService(final ModifiableXaDataSource dataSourceConfig, final Map<String, Supplier<String>> xaDataSourceProperties) {
        this.dataSourceConfig = dataSourceConfig;
        this.xaDataSourceProperties = xaDataSourceProperties;
    }

    public void start(final StartContext startContext) throws StartException {
        for (Map.Entry<String, Supplier<String>> xaDataSourceProperty : xaDataSourceProperties.entrySet()) {
            dataSourceConfig.addXaDataSourceProperty(xaDataSourceProperty.getKey(), xaDataSourceProperty.getValue().get());
        }
    }

    public void stop(final StopContext stopContext) {
    }

    @Override
    public ModifiableXaDataSource getValue() {
        return dataSourceConfig;
    }

}