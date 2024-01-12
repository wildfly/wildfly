/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        final T dataSource = getDataSourceConfig(context, address);

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

    private T getDataSourceConfig(final OperationContext context, final PathAddress operationAddress) throws OperationFailedException {

        final List<PathElement> relativeAddress = new ArrayList<PathElement>();
        for (int i = operationAddress.size() - 1; i >= 0; i--) {
            PathElement pe = operationAddress.getElement(i);
            if (ModelDescriptionConstants.DEPLOYMENT.equals(pe.getKey())) {
                String runtimeName = getRuntimeName(context, pe);
                pe = PathElement.pathElement(pe.getKey(), runtimeName);
            }
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

    private static String getRuntimeName(final OperationContext context, final PathElement element) throws OperationFailedException {
        final ModelNode deploymentModel = context.readResourceFromRoot(PathAddress.pathAddress(element), false).getModel();
        if (!deploymentModel.hasDefined(ModelDescriptionConstants.RUNTIME_NAME)) {
            String exceptionMessage = ConnectorLogger.ROOT_LOGGER.noDataSourceRegisteredForAddress(context.getCurrentAddress());
            throw new OperationFailedException(exceptionMessage);
        }
        return deploymentModel.get(ModelDescriptionConstants.RUNTIME_NAME).asString();
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
