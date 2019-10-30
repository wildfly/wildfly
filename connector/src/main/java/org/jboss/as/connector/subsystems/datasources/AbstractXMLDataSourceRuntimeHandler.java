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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Base type for runtime operations on datasources and XA datasources
 *
 * @author Stuart Douglas
 */
public abstract class AbstractXMLDataSourceRuntimeHandler<T> extends AbstractRuntimeOnlyHandler {

    protected static final String CONNECTION_PROPERTIES = "connection-properties";
    protected static final String XA_DATASOURCE_PROPERTIES = "xa-datasource-properties";
    protected static final String DATA_SOURCE = "data-source";
    protected static final String XA_DATA_SOURCE = "xa-data-source";

    private final Map<PathAddress, T> dataSourceConfigs = Collections.synchronizedMap(new HashMap<PathAddress, T>());

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        String opName = operation.require(ModelDescriptionConstants.OP).asString();
        PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final T dataSource = getDataSourceConfig(address);

        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeReadAttribute(attributeName, context, dataSource, address);
        } else {
            throw unknownOperation(opName);
        }
    }

    public void registerDataSource(final PathAddress address, final T dataSource) {
        dataSourceConfigs.put(address, dataSource);
    }

    public void unregisterDataSource(final PathAddress address) {
        dataSourceConfigs.remove(address);
    }

    protected abstract void executeReadAttribute(final String attributeName, final OperationContext context, final T dataSource, final PathAddress address);

    private static IllegalStateException unknownOperation(String opName) {
        throw ConnectorLogger.ROOT_LOGGER.unknownOperation(opName);
    }

    private T getDataSourceConfig(final PathAddress operationAddress) throws OperationFailedException {

        final List<PathElement> relativeAddress = new ArrayList<PathElement>();
        for (int i = operationAddress.size() - 1; i >= 0; i--) {
            PathElement pe = operationAddress.getElement(i);
            relativeAddress.add(0, pe);
            if (ModelDescriptionConstants.DEPLOYMENT.equals(pe.getKey())) {
                break;
            }
        }

        final PathAddress pa = PathAddress.pathAddress(relativeAddress);
        final T config;
        if(operationAddress.getLastElement().getKey().equals(CONNECTION_PROPERTIES) ||
                operationAddress.getLastElement().getKey().equals(XA_DATASOURCE_PROPERTIES)) {
            config = dataSourceConfigs.get(pa.subAddress(0, pa.size() - 1));
        } else {
           config = dataSourceConfigs.get(pa);
        }
        if (config == null) {
            String exceptionMessage = ConnectorLogger.ROOT_LOGGER.noDataSourceRegisteredForAddress(operationAddress);
            throw new OperationFailedException(exceptionMessage);
        }
        return config;
    }


    protected void setLongIfNotNull(final OperationContext context, final Long value) {
        if (value != null) {
            context.getResult().set(value);
        }
    }

    protected void setIntIfNotNull(final OperationContext context, final Integer value) {
        if (value != null) {
            context.getResult().set(value);
        }
    }

    protected void setBooleanIfNotNull(final OperationContext context, final Boolean value) {
        if (value != null) {
            context.getResult().set(value);
        }
    }

    protected void setStringIfNotNull(final OperationContext context, final String value) {
        if (value != null) {
            context.getResult().set(value);
        }
    }
}
