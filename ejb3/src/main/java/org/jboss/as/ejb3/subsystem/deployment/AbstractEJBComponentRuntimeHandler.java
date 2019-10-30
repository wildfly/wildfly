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
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.pool.Pool;
import org.jboss.as.ejb3.security.EJBSecurityMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
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
        final ServiceName serviceName = getComponentConfiguration(context,address);
        T component = getComponent(serviceName, address, context, forWrite);

        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeReadAttribute(attributeName, context, component,  address);
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
            final Pool<?> pool = componentType.getPool(component);
            final ModelNode result = context.getResult();
            if (pool != null) {
                result.set(pool.getAvailableCount());
            }
        } else if (hasPool && POOL_CREATE_COUNT.getName().equals(attributeName)) {
            final Pool<?> pool = componentType.getPool(component);
            final ModelNode result = context.getResult();
            if (pool != null) {
                result.set(pool.getCreateCount());
            }
        } else if (hasPool && POOL_NAME.getName().equals(attributeName)) {
            final String poolName = componentType.pooledComponent(component).getPoolName();
            final ModelNode result = context.getResult();
            if (poolName != null) {
                result.set(poolName);
            }
        } else if (hasPool && POOL_REMOVE_COUNT.getName().equals(attributeName)) {
            final Pool<?> pool = componentType.getPool(component);
            final ModelNode result = context.getResult();
            if (pool != null) {
                result.set(pool.getRemoveCount());
            }
        } else if (hasPool && POOL_CURRENT_SIZE.getName().equals(attributeName)) {
            final Pool<?> pool = componentType.getPool(component);
            final ModelNode result = context.getResult();
            if (pool != null) {
                result.set(pool.getCurrentSize());
            }
        } else if (hasPool && POOL_MAX_SIZE.getName().equals(attributeName)) {
            final Pool<?> pool = componentType.getPool(component);
            final ModelNode result = context.getResult();
            if (pool != null) {
                result.set(pool.getMaxSize());
            }
        } else {
            // Bug; we were registered for an attribute but there is no code for handling it
            throw EjbLogger.ROOT_LOGGER.unknownAttribute(attributeName);
        }
    }

    protected void executeWriteAttribute(String attributeName, OperationContext context, ModelNode operation, T component,
                                         PathAddress address) throws OperationFailedException {
        if (componentType.hasPool() && POOL_MAX_SIZE.getName().equals(attributeName)) {
            int newSize = POOL_MAX_SIZE.resolveValue(context, operation.get(VALUE)).asInt();
            final Pool<?> pool = componentType.getPool(component);
            final int oldSize = pool.getMaxSize();
            componentType.getPool(component).setMaxSize(newSize);
            context.completeStep(new OperationContext.RollbackHandler() {
                @Override
                public void handleRollback(OperationContext context, ModelNode operation) {
                    pool.setMaxSize(oldSize);
                }
            });
        } else {
            // Bug; we were registered for an attribute but there is no code for handling it
            throw EjbLogger.ROOT_LOGGER.unknownAttribute(attributeName);
        }
    }

    protected void executeAgainstComponent(OperationContext context, ModelNode operation, T component, String opName, PathAddress address) throws OperationFailedException {
        throw unknownOperation(opName);
    }

    protected boolean isOperationReadOnly(String opName) {
        throw unknownOperation(opName);
    }

    private static IllegalStateException unknownOperation(String opName) {
        throw EjbLogger.ROOT_LOGGER.unknownOperations(opName);
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

    private ServiceName getComponentConfiguration(final OperationContext context, final PathAddress operationAddress) throws OperationFailedException {

      final List<PathElement> relativeAddress = new ArrayList<PathElement>();
      final String typeKey = this.componentType.getResourceType();
      boolean skip=true;
      for (int i = operationAddress.size() - 1; i >= 0; i--) {
          PathElement pe = operationAddress.getElement(i);
          if(skip && !pe.getKey().equals(typeKey)){
              continue;
          } else {
              skip = false;
          }

          if (ModelDescriptionConstants.DEPLOYMENT.equals(pe.getKey())) {
              final String runtimName = resolveRuntimeName(context,pe);
              PathElement realPe = PathElement.pathElement(pe.getKey(), runtimName);
              relativeAddress.add(0, realPe);
              break;
          } else {
              relativeAddress.add(0, pe);
          }
      }

      final PathAddress pa = PathAddress.pathAddress(relativeAddress);
      final ServiceName config = componentConfigs.get(pa);
      if (config == null) {
          String exceptionMessage = EjbLogger.ROOT_LOGGER.noComponentRegisteredForAddress(operationAddress);
          throw new OperationFailedException(exceptionMessage);
      }

      return config;
  }

    private T getComponent(final ServiceName serviceName, final PathAddress operationAddress,
                           final OperationContext context, final boolean forWrite) throws OperationFailedException {

        ServiceRegistry registry = context.getServiceRegistry(forWrite);
        ServiceController<?> controller = registry.getService(serviceName);
        if (controller == null) {
            String exceptionMessage = EjbLogger.ROOT_LOGGER.noComponentAvailableForAddress(operationAddress);
            throw new OperationFailedException(exceptionMessage);
        }
        ServiceController.State controllerState = controller.getState();
        if (controllerState != ServiceController.State.UP) {
            String exceptionMessage = EjbLogger.ROOT_LOGGER.invalidComponentState(operationAddress, controllerState, ServiceController.State.UP);
            throw new OperationFailedException(exceptionMessage);
        }
        return componentClass.cast(controller.getValue());
    }

    T getComponent(OperationContext context, ModelNode operation) throws OperationFailedException{
        PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final ServiceName serviceName = getComponentConfiguration(context,address);
        T component = getComponent(serviceName, address, context, false);
        return component;
    }

    /**
     * Resolves runtime name of model resource.
     * @param context - operation context in which handler is invoked
     * @param address - deployment address
     * @return runtime name of module. Value which is returned is never null.
     */
    protected String resolveRuntimeName(final OperationContext context, final PathElement address){
        final ModelNode runtimeName = context.readResourceFromRoot(PathAddress.pathAddress(address),false).getModel()
                .get(ModelDescriptionConstants.RUNTIME_NAME);
            return runtimeName.asString();
    }
}
