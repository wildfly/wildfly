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

package org.jboss.as.ejb3.subsystem.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.security.EJBSecurityMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.COMPONENT_CLASS_NAME;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.DECLARED_ROLES;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_AVAILABLE_COUNT;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_CREATE_COUNT;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_CURRENT_SIZE;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_MAX_SIZE;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_NAME;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_REMOVE_COUNT;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.RUN_AS_ROLE;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.SECURITY_DOMAIN;
/**
 * Base class for operation handlers that provide runtime management for {@link EJBComponent}s.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractEJBComponentRuntimeHandler<T extends EJBComponent> extends AbstractRuntimeOnlyHandler {

    private final Map<PathAddress, ServiceName> componentConfigs = Collections.synchronizedMap(new HashMap<PathAddress, ServiceName>());

    private final Class<T> componentClass;
    private final EJBComponentType componentType;

    protected AbstractEJBComponentRuntimeHandler(final EJBComponentType componentType, final Class<T> componentClass) {
        this.componentType = componentType;
        this.componentClass = componentClass;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        String opName = operation.require(ModelDescriptionConstants.OP).asString();
        boolean forWrite = isForWrite(opName);
        PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final ServiceName serviceName = getComponentConfiguration(address);
        T component = getComponent(serviceName, address, context, forWrite);

        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeReadAttribute(attributeName, context, component,  address);
            context.completeStep();
        } else if (ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeWriteAttribute(attributeName, context, operation, component, address);
        } else {
            executeAgainstComponent(context, operation, component,  opName, address);
        }
    }

    public void registerComponent(final PathAddress address, final ServiceName serviceName) {
        componentConfigs.put(address, serviceName);
    }

    public void unregisterComponent(final PathAddress address) {
        componentConfigs.remove(address);
    }

    protected void executeReadAttribute(final String attributeName, final OperationContext context, final T component, final PathAddress address) {
        final boolean hasPool = componentType.hasPool();
        if (COMPONENT_CLASS_NAME.getName().equals(attributeName)) {
            context.getResult().set(component.getComponentName());
        } else if (SECURITY_DOMAIN.getName().equals(attributeName)) {
            final ModelNode result = context.getResult();
            EJBSecurityMetaData md = component.getSecurityMetaData();
            if (md != null && md.getSecurityDomain() != null) {
                result.set(md.getSecurityDomain());
            }
        } else if (RUN_AS_ROLE.getName().equals(attributeName)) {
            final ModelNode result = context.getResult();
            EJBSecurityMetaData md = component.getSecurityMetaData();
            if (md != null && md.getRunAs() != null) {
                result.set(md.getRunAs());
            }
        } else if (DECLARED_ROLES.getName().equals(attributeName)) {
            final ModelNode result = context.getResult();
            EJBSecurityMetaData md = component.getSecurityMetaData();
            if (md != null) {
                result.setEmptyList();
                Set<String> roles = md.getDeclaredRoles();
                if (roles != null) {
                    for (String role : roles) {
                        result.add(role);
                    }
                }
            }
        } else if (componentType.hasTimer() && TimerAttributeDefinition.INSTANCE.getName().equals(attributeName)) {
            TimerAttributeDefinition.addTimers(component, context.getResult());
        } else if (hasPool && POOL_AVAILABLE_COUNT.getName().equals(attributeName)) {
            int count = componentType.getPool(component).getAvailableCount();
            context.getResult().set(count);
        } else if (hasPool && POOL_CREATE_COUNT.getName().equals(attributeName)) {
            int count = componentType.getPool(component).getCreateCount();
            context.getResult().set(count);
        } else if (hasPool && POOL_NAME.getName().equals(attributeName)) {
            final String poolName = componentType.pooledComponent(component).getPoolName();
            context.getResult().set(poolName);
        } else if (hasPool && POOL_REMOVE_COUNT.getName().equals(attributeName)) {
            int count = componentType.getPool(component).getRemoveCount();
            context.getResult().set(count);
        } else if (hasPool && POOL_CURRENT_SIZE.getName().equals(attributeName)) {
            int size = componentType.getPool(component).getCurrentSize();
            context.getResult().set(size);
        } else if (hasPool && POOL_MAX_SIZE.getName().equals(attributeName)) {
            int size = componentType.getPool(component).getMaxSize();
            context.getResult().set(size);
        } else {
            // Bug; we were registered for an attribute but there is no code for handling it
            throw new IllegalStateException(String.format("Unknown attribute %s", attributeName));
        }
    }

    protected void executeWriteAttribute(String attributeName, OperationContext context, ModelNode operation, T component,
                                         PathAddress address) throws OperationFailedException {
        if (componentType.hasPool() && POOL_MAX_SIZE.getName().equals(attributeName)) {
            int newSize = POOL_MAX_SIZE.resolveModelAttribute(context, operation).asInt();
            Pool<?> pool = componentType.getPool(component);
            int oldSize = pool.getMaxSize();
            componentType.getPool(component).setMaxSize(newSize);
            if (context.completeStep() != OperationContext.ResultAction.KEEP) {
                pool.setMaxSize(oldSize);
            }
        } else {
            // Bug; we were registered for an attribute but there is no code for handling it
            throw MESSAGES.unknownAttribute(attributeName);
        }
    }

    protected void executeAgainstComponent(OperationContext context, ModelNode operation, T component, String opName, PathAddress address) throws OperationFailedException {
        throw unknownOperation(opName);
    }

    protected boolean isOperationReadOnly(String opName) {
        throw unknownOperation(opName);
    }

    private static IllegalStateException unknownOperation(String opName) {
        throw MESSAGES.unknownOperations(opName);
    }

    private boolean isForWrite(String opName) {
        if (ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION.equals(opName)) {
            return true;
        } else if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            return false;
        } else {
            return !isOperationReadOnly(opName);
        }
    }

    private ServiceName getComponentConfiguration(final PathAddress operationAddress) throws OperationFailedException {

        final List<PathElement> relativeAddress = new ArrayList<PathElement>();
        for (int i = operationAddress.size() - 1; i >= 0; i--) {
            PathElement pe = operationAddress.getElement(i);
            relativeAddress.add(0, pe);
            if (ModelDescriptionConstants.DEPLOYMENT.equals(pe.getKey())) {
                break;
            }
        }

        final PathAddress pa = PathAddress.pathAddress(relativeAddress);
        final ServiceName config = componentConfigs.get(pa);
        if (config == null) {
            String exceptionMessage = MESSAGES.noComponentRegisteredForAddress(operationAddress);
            throw new OperationFailedException(new ModelNode().set(exceptionMessage));
        }

        return config;
    }

    private T getComponent(final ServiceName serviceName, final PathAddress operationAddress,
                           final OperationContext context, final boolean forWrite) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(forWrite);
        ServiceController<?> controller = registry.getService(serviceName);
        if (controller == null) {
            String exceptionMessage = MESSAGES.noComponentAvailableForAddress(operationAddress);
            throw new OperationFailedException(new ModelNode().set(exceptionMessage));
        }
        ServiceController.State controllerState = controller.getState();
        if (controllerState != ServiceController.State.UP) {
            String exceptionMessage = MESSAGES.invalidComponentState(operationAddress,controllerState,ServiceController.State.UP);
            throw new OperationFailedException(new ModelNode().set(exceptionMessage));
        }
        return componentClass.cast(controller.getValue());
    }
}
