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

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.from;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.DATASOURCE_ATTRIBUTE;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.validator.ValidateException;

/**
 * Operation handler responsible for adding a DataSource.
 *
 * @author John Bailey
 */
public class DataSourceAdd extends AbstractDataSourceAdd {
    static final DataSourceAdd INSTANCE = new DataSourceAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        populateAddModel(operation, model, CONNECTION_PROPERTIES, DATASOURCE_ATTRIBUTE);
    }

    protected AbstractDataSourceService createDataSourceService(final String jndiName, final ModelNode operation) throws OperationFailedException {
        final DataSource dataSource;
        try {
            dataSource = from(operation);
        } catch (ValidateException e) {
            throw new OperationFailedException(e, new ModelNode().set("Failed to create DataSource instance for [" + operation + "]"));
        }
        return new LocalDataSourceService(jndiName, dataSource);
    }
}
