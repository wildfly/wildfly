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

import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCEPROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.xaFrom;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.validator.ValidateException;

/**
 * Operation handler responsible for adding a XA data-source.
 * @author John Bailey
 */
public class XaDataSourceAdd extends AbstractDataSourceAdd {
    static final XaDataSourceAdd INSTANCE = new XaDataSourceAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        populateAddModel(operation, model, XADATASOURCEPROPERTIES, XA_DATASOURCE_ATTRIBUTE);
    }

    protected AbstractDataSourceService createDataSourceService(final String jndiName, final ModelNode operation)
            throws OperationFailedException {
        final XaDataSource dataSource;
        try {
            dataSource = xaFrom(operation);
        } catch (ValidateException e) {
            throw new OperationFailedException(e, new ModelNode().set("Failed to create XaDataSource instance for ["
                    + operation + "]\n reason:" + e.getLocalizedMessage()));
        }
        return new XaDataSourceService(jndiName, dataSource);
    }
}
