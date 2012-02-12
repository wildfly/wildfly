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

import static org.jboss.as.connector.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.DriverRegistry;
import org.jboss.as.connector.registry.DriverService;
import org.jboss.as.connector.registry.InstalledDriver;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for adding a jdbc driver.
 *
 * @author John Bailey
 */
public class JdbcDriverAdd extends AbstractAddStepHandler {
    static final JdbcDriverAdd INSTANCE = new JdbcDriverAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String driverName = operation.require(DRIVER_NAME.getName()).asString();
        final String moduleName = operation.require(DRIVER_MODULE_NAME.getName()).asString();

        final Integer majorVersion = operation.hasDefined(DRIVER_MAJOR_VERSION.getName()) ? operation.get(DRIVER_MAJOR_VERSION.getName()).asInt() : null;
        final Integer minorVersion = operation.hasDefined(DRIVER_MINOR_VERSION.getName()) ? operation.get(DRIVER_MINOR_VERSION.getName()).asInt() : null;
        final String driverClassName = operation.hasDefined(DRIVER_CLASS_NAME.getName()) ? operation.get(DRIVER_CLASS_NAME.getName()).asString() : null;
        final String dataSourceClassName = operation.hasDefined(DRIVER_DATASOURCE_CLASS_NAME.getName()) ? operation.get(DRIVER_DATASOURCE_CLASS_NAME.getName()).asString() : null;
        final String xaDataSourceClassName = operation.hasDefined(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()) ? operation.get(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()).asString() : null;

        //Apply to the model
        model.get(DRIVER_NAME.getName()).set(driverName);
        model.get(DRIVER_MODULE_NAME.getName()).set(moduleName);
        if (majorVersion != null)
            model.get(DRIVER_MAJOR_VERSION.getName()).set(majorVersion);
        if (minorVersion != null)
            model.get(DRIVER_MINOR_VERSION.getName()).set(minorVersion);
        if (driverClassName != null)
            model.get(DRIVER_CLASS_NAME.getName()).set(driverClassName);
        if (dataSourceClassName != null)
            model.get(DRIVER_DATASOURCE_CLASS_NAME.getName()).set(dataSourceClassName);
        if (xaDataSourceClassName != null)
            model.get(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()).set(xaDataSourceClassName);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String driverName = operation.require(DRIVER_NAME.getName()).asString();
        String moduleName = operation.require(DRIVER_MODULE_NAME.getName()).asString();
        final Integer majorVersion = operation.hasDefined(DRIVER_MAJOR_VERSION.getName()) ? operation.get(DRIVER_MAJOR_VERSION.getName()).asInt() : null;
        final Integer minorVersion = operation.hasDefined(DRIVER_MINOR_VERSION.getName()) ? operation.get(DRIVER_MINOR_VERSION.getName()).asInt() : null;
        final String driverClassName = operation.hasDefined(DRIVER_CLASS_NAME.getName()) ? operation.get(DRIVER_CLASS_NAME.getName()).asString() : null;
        final String dataSourceClassName = operation.hasDefined(DRIVER_DATASOURCE_CLASS_NAME.getName()) ? operation.get(DRIVER_DATASOURCE_CLASS_NAME.getName()).asString() : null;
        final String xaDataSourceClassName = operation.hasDefined(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()) ? operation.get(
                DRIVER_XA_DATASOURCE_CLASS_NAME.getName()).asString() : null;

        final ServiceTarget target = context.getServiceTarget();

        final ModuleIdentifier moduleId;
        final Module module;
        String slot = null;
        if (moduleName.contains(":")) {
            slot = moduleName.substring(moduleName.indexOf(":") + 1);
            moduleName = moduleName.substring(0, moduleName.indexOf(":"));
        }

        try {
            moduleId = ModuleIdentifier.create(moduleName, slot);
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleLoadException e) {
            context.getFailureDescription().set(MESSAGES.failedToLoadModuleDriver(moduleName));
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
                throw new OperationFailedException(new ModelNode().set(MESSAGES.cannotInstantiateDriverClass(driverClassName)));
            }
        }
    }

    public static void startDriverServices(final ServiceTarget target, final ModuleIdentifier moduleId, Driver driver, final String driverName, final Integer majorVersion, final Integer minorVersion, final String dataSourceClassName, final String xaDataSourceClassName)
            throws IllegalStateException {
        final int majorVer = driver.getMajorVersion();
        final int minorVer = driver.getMinorVersion();
        if ((majorVersion != null && majorVersion.intValue() != majorVer)
                || (minorVersion != null && minorVersion.intValue() != minorVer)) {
            throw MESSAGES.driverVersionMismatch();
        }

        final boolean compliant = driver.jdbcCompliant();
        if (compliant) {
            SUBSYSTEM_DATASOURCES_LOGGER.deployingCompliantJdbcDriver(driver.getClass(), Integer.valueOf(majorVer),
                    Integer.valueOf(minorVer));
        } else {
            SUBSYSTEM_DATASOURCES_LOGGER.deployingNonCompliantJdbcDriver(driver.getClass(), Integer.valueOf(majorVer),
                    Integer.valueOf(minorVer));
        }
        InstalledDriver driverMetadata = new InstalledDriver(driverName, moduleId, driver.getClass().getName(),
            dataSourceClassName, xaDataSourceClassName, majorVer, minorVer, compliant);
        DriverService driverService = new DriverService(driverMetadata, driver);
        target.addService(ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_")), driverService)
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        driverService.getDriverRegistryServiceInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE).install();
    }


}
