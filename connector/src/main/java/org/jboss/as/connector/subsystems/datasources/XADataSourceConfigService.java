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