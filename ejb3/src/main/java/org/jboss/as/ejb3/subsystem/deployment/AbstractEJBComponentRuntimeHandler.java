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

import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.COMPONENT_CLASS_NAME;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.DECLARED_ROLES;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_AVAILABLE_COUNT;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_CREATE_COUNT;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_CURRENT_SIZE;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_MAX_SIZE;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.POOL_REMOVE_COUNT;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.RUN_AS_ROLE;
import static org.jboss.as.ejb3.subsystem.deployment.AbstractEJBComponentResourceDefinition.SECURITY_DOMAIN;

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
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.security.EJBSecurityMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Base class for operation handlers that provide runtime management for {@link EJBComponent}s.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractEJBComponentRuntimeHandler<T extends EJBComponent> extends AbstractRuntimeOnlyHandler {

    private final Map<PathAddress, ComponentConfiguration> componentConfigs = Collections.synchronizedMap(new HashMap<PathAddress, ComponentConfiguration>());

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
        ComponentConfiguration config = getComponentConfiguration(address);
        T component = getComponent(config, address, context, forWrite);

        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeReadAttribute(attributeName, context, component, config, address);
            context.completeStep();
        } else if (ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeWriteAttribute(attributeName, context, operation, component, config, address);
        } else {
            executeAgainstComponent(context, operation, component, config, opName, address);
        }
    }

    public void registerComponent(final PathAddress address, final ComponentConfiguration configuration) {
        componentConfigs.put(address, configuration);
    }

    public void unregisterComponent(final PathAddress address) {
        componentConfigs.remove(address);
    }

    protected void executeReadAttribute(String attributeName, OperationContext context, T component,
                                        ComponentConfiguration config, PathAddress address) {
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
                                        ComponentConfiguration config, PathAddress address) throws OperationFailedException {
        if (componentType.hasPool() && POOL_MAX_SIZE.getName().equals(attributeName)) {
            int newSize = POOL_MAX_SIZE.validateResolvedOperation(operation).asInt();
            Pool<?> pool = componentType.getPool(component);
            int oldSize = pool.getMaxSize();
            componentType.getPool(component).setMaxSize(newSize);
            if (context.completeStep() != OperationContext.ResultAction.KEEP) {
                pool.setMaxSize(oldSize);
            }
        } else {
            // Bug; we were registered for an attribute but there is no code for handling it
            throw new IllegalStateException(String.format("Unknown attribute %s", attributeName));
        }
    }

    protected void executeAgainstComponent(OperationContext context, ModelNode operation, T component,
                                           ComponentConfiguration componentConfiguration, String opName, PathAddress address) throws OperationFailedException {
        throw unknownOperation(opName);
    }

    protected boolean isOperationReadOnly(String opName) {
        throw unknownOperation(opName);
    }

    private static IllegalStateException unknownOperation(String opName) {
        throw new IllegalStateException((String.format("Unknown operation %s", opName)));
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

    private ComponentConfiguration getComponentConfiguration(final PathAddress operationAddress) throws OperationFailedException {

        List<PathElement> relativeAddress = new ArrayList<PathElement>();
        for (int i = operationAddress.size() - 1; i >= 0; i--) {
            PathElement pe = operationAddress.getElement(i);
            relativeAddress.add(0, pe);
            if (ModelDescriptionConstants.DEPLOYMENT.equals(pe.getKey())) {
                break;
            }
        }

        PathAddress pa = PathAddress.pathAddress(relativeAddress);
        ComponentConfiguration config = componentConfigs.get(pa);
        if (config == null) {
            throw new OperationFailedException(new ModelNode().set(String.format("No EJB component registered for address %s", operationAddress)));
        }

        return config;
    }

    private T getComponent(final ComponentConfiguration config, final PathAddress operationAddress,
                           final OperationContext context, final boolean forWrite) throws OperationFailedException {

        ServiceName createServiceName = config.getComponentDescription().getCreateServiceName();
        ServiceRegistry registry = context.getServiceRegistry(forWrite);
        ServiceController<?> controller = registry.getService(createServiceName);
        if (controller == null) {
            throw new OperationFailedException(new ModelNode().set(String.format("No EJB component is available for address %s", operationAddress)));
        }
        ServiceController.State controllerState = controller.getState();
        if (controllerState != ServiceController.State.UP) {
            throw new OperationFailedException(new ModelNode().set(String.format("EJB component for address %s is in " +
                    "state %s, must be in state %s", operationAddress, controllerState, ServiceController.State.UP)));
        }
        return componentClass.cast(controller.getValue());
    }
}
