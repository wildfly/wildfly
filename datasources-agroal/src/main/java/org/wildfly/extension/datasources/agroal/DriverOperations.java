/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.extension.datasources.agroal.logging.AgroalLogger;

import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.ServiceLoader;

/**
 * Handler responsible for adding a driver resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class DriverOperations {


    static final OperationStepHandler ADD_OPERATION = new DriverAdd();

    static final OperationStepHandler REMOVE_OPERATION = new DriverRemove();

    static final OperationStepHandler INFO_OPERATION = new DriverInfo();

    // --- //

    private static class DriverAdd extends AbstractAddStepHandler {

        private DriverAdd() {
            super(DriverDefinition.ATTRIBUTES);
        }

        private static Class<?> loadClass(String driverName, String moduleName, String className) throws IllegalArgumentException {
            try {
                Module module = Module.getCallerModuleLoader().loadModule(moduleName);
                AgroalLogger.DRIVER_LOGGER.debugf("loaded module '%s' for driver: %s", moduleName, driverName);
                Class<?> providerClass = module.getClassLoader().loadClass(className);
                AgroalLogger.DRIVER_LOGGER.driverLoaded(className, driverName);
                return providerClass;
            } catch (ModuleLoadException e) {
                throw AgroalLogger.DRIVER_LOGGER.loadModuleException(e, moduleName);
            } catch (ClassNotFoundException e) {
                throw AgroalLogger.DRIVER_LOGGER.loadClassException(e, className);
            }
        }

        private static Class<?> loadDriver(String driverName, String moduleName) throws IllegalArgumentException {
            try {
                Module module = Module.getCallerModuleLoader().loadModule(moduleName);
                AgroalLogger.DRIVER_LOGGER.debugf("loaded module '%s' for driver: %s", moduleName, driverName);

                ServiceLoader<Driver> serviceLoader = module.loadService(Driver.class);
                if (serviceLoader.iterator().hasNext()) {
                    // Consider just the first definition. User can use different implementation only with explicit declaration of class attribute
                    Class<?> driverClass = serviceLoader.iterator().next().getClass();
                    AgroalLogger.DRIVER_LOGGER.driverLoaded(driverClass.getName(), driverName);
                    return driverClass;
                }
                return null;
            } catch (ModuleLoadException e) {
                throw AgroalLogger.DRIVER_LOGGER.loadModuleException(e, moduleName);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String driverName = context.getCurrentAddressValue();

            String moduleName = DriverDefinition.MODULE_ATTRIBUTE.resolveModelAttribute(context, model).asString();

            boolean classDefined = DriverDefinition.CLASS_ATTRIBUTE.resolveModelAttribute(context, model).isDefined();
            String className = classDefined ? DriverDefinition.CLASS_ATTRIBUTE.resolveModelAttribute(context, model).asString() : null;

            Service<?> driverService = new ValueService<>(new Value<Class<?>>() {
                @Override
                public Class<?> getValue() throws IllegalStateException, IllegalArgumentException {
                    if (className != null) {
                        return loadClass(driverName, moduleName, className);
                    } else {
                        return loadDriver(driverName, moduleName);
                    }
                }
            });

            context.getCapabilityServiceTarget().addCapability(DriverDefinition.AGROAL_DRIVER_CAPABILITY.fromBaseCapability(driverName), driverService).install();
        }
    }

    // --- //

    private static class DriverRemove extends AbstractRemoveStepHandler {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            context.removeService(DriverDefinition.AGROAL_DRIVER_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress()));
            AgroalLogger.DRIVER_LOGGER.debugf("unloaded driver: %s", context.getCurrentAddressValue());
        }
    }

    // --- //

    private static class DriverInfo implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                Class<?> providerClass = (Class<?>) context.getServiceRegistry(false).getService(DriverDefinition.AGROAL_DRIVER_CAPABILITY.getCapabilityServiceName(context.getCurrentAddress())).getValue();

                if (providerClass == null) {
                    context.getResult().set(new ModelNode());
                    return;
                }

                ModelNode result = new ModelNode();

                // consistent with io.agroal.pool.util.PropertyInjector
                if (javax.sql.XADataSource.class.isAssignableFrom(providerClass) || javax.sql.DataSource.class.isAssignableFrom(providerClass)) {
                    for (Method method : providerClass.getMethods()) {
                        String name = method.getName();
                        if (method.getParameterCount() == 1 && name.startsWith("set")) {
                            result.get(name.substring(3)).set(method.getParameterTypes()[0].getName());
                        }
                    }
                }

                // from java.sql.Driver maybe use Driver.getPropertyInfo

                context.getResult().set(result);
            }
        }
    }
}
