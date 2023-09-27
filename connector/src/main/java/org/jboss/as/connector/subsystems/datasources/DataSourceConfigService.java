/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
public class DataSourceConfigService implements Service<ModifiableDataSource> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("data-source-config");
    private final ModifiableDataSource dataSourceConfig;
    private final Map<String, Supplier<String>> connectionProperties;

    public DataSourceConfigService(final ModifiableDataSource dataSourceConfig, final Map<String, Supplier<String>> connectionProperties) {
        this.dataSourceConfig = dataSourceConfig;
        this.connectionProperties = connectionProperties;
    }

    public void start(final StartContext startContext) throws StartException {
        for (Map.Entry<String, Supplier<String>> connectionProperty : connectionProperties.entrySet()) {
            dataSourceConfig.addConnectionProperty(connectionProperty.getKey(), connectionProperty.getValue().get());
        }
    }

    public void stop(final StopContext stopContext) {
    }

    @Override
    public ModifiableDataSource getValue() {
        return dataSourceConfig;
    }

}
