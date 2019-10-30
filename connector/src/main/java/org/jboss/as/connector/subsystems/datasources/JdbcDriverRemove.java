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

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.JdbcDriverAdd.startDriverServices;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.util.ServiceLoader;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for removing a jdbc driver.
 *
 * @author John Bailey
 */
public class JdbcDriverRemove extends AbstractRemoveStepHandler {
    static final JdbcDriverRemove INSTANCE = new JdbcDriverRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String driverName = model.get(DRIVER_NAME.getName()).asString();

        final ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceName  jdbcServiceName = ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_"));
        ServiceController<?> jdbcServiceController = registry.getService(jdbcServiceName);
        context.removeService(jdbcServiceController);

    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        final String driverName = model.require(DRIVER_NAME.getName()).asString();
        final String moduleName = model.require(DRIVER_MODULE_NAME.getName()).asString();
        final Integer majorVersion = model.hasDefined(DRIVER_MAJOR_VERSION.getName()) ? model.get(DRIVER_MAJOR_VERSION.getName()).asInt() : null;
        final Integer minorVersion = model.hasDefined(DRIVER_MINOR_VERSION.getName()) ? model.get(DRIVER_MINOR_VERSION.getName()).asInt() : null;
        final String driverClassName = model.hasDefined(DRIVER_CLASS_NAME.getName()) ? model.get(DRIVER_CLASS_NAME.getName()).asString() : null;
        final String dataSourceClassName = model.hasDefined(DRIVER_DATASOURCE_CLASS_NAME.getName()) ? model.get(DRIVER_DATASOURCE_CLASS_NAME.getName()).asString() : null;
        final String xaDataSourceClassName = model.hasDefined(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()) ? model.get(
                DRIVER_XA_DATASOURCE_CLASS_NAME.getName()).asString() : null;

        final ServiceTarget target = context.getServiceTarget();

        final ModuleIdentifier moduleId;
        final Module module;
        try {
            moduleId = ModuleIdentifier.fromString(moduleName);
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleNotFoundException e) {
            context.getFailureDescription().set(ConnectorLogger.ROOT_LOGGER.missingDependencyInModuleDriver(moduleName, e.getMessage()));
            return;
        } catch (ModuleLoadException e) {
            context.getFailureDescription().set(ConnectorLogger.ROOT_LOGGER.failedToLoadModuleDriver(moduleName));
            return;
        }

        if (driverClassName == null) {
            final ServiceLoader<Driver> serviceLoader = module.loadService(Driver.class);
            if (serviceLoader != null)
                for (Driver driver : serviceLoader) {
                    startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName, xaDataSourceClassName);
                }
        } else {
            try {
                final Class<? extends Driver> driverClass = module.getClassLoader().loadClass(driverClassName)
                        .asSubclass(Driver.class);
                final Constructor<? extends Driver> constructor = driverClass.getConstructor();
                final Driver driver = constructor.newInstance();
                startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName, xaDataSourceClassName);
            } catch (Exception e) {
                SUBSYSTEM_DATASOURCES_LOGGER.cannotInstantiateDriverClass(driverClassName, e);

            }
        }
    }
}
