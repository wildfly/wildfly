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
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.util.ServiceLoader;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.driver.DriverService;
import org.jboss.as.connector.services.driver.InstalledDriver;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceBuilder;
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

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final String driverName = PathAddress.pathAddress(address).getLastElement().getValue();

        for (AttributeDefinition attribute : Constants.JDBC_DRIVER_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
        model.get(DRIVER_NAME.getName()).set(driverName);//this shouldn't be here anymore
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final String driverName = PathAddress.pathAddress(address).getLastElement().getValue();
        if (operation.get(DRIVER_NAME.getName()).isDefined() && !driverName.equals(operation.get(DRIVER_NAME.getName()).asString())) {
            throw ConnectorLogger.ROOT_LOGGER.driverNameAndResourceNameNotEquals(operation.get(DRIVER_NAME.getName()).asString(), driverName);
        }
        String moduleName = DRIVER_MODULE_NAME.resolveModelAttribute(context, operation).asString();
        final Integer majorVersion = operation.hasDefined(DRIVER_MAJOR_VERSION.getName()) ? DRIVER_MAJOR_VERSION.resolveModelAttribute(context, operation).asInt() : null;
        final Integer minorVersion = operation.hasDefined(DRIVER_MINOR_VERSION.getName()) ? DRIVER_MINOR_VERSION.resolveModelAttribute(context, operation).asInt() : null;
        final String driverClassName = operation.hasDefined(DRIVER_CLASS_NAME.getName()) ? DRIVER_CLASS_NAME.resolveModelAttribute(context, operation).asString() : null;
        final String dataSourceClassName = operation.hasDefined(DRIVER_DATASOURCE_CLASS_NAME.getName()) ? DRIVER_DATASOURCE_CLASS_NAME.resolveModelAttribute(context, operation).asString() : null;
        final String xaDataSourceClassName = operation.hasDefined(DRIVER_XA_DATASOURCE_CLASS_NAME.getName()) ? DRIVER_XA_DATASOURCE_CLASS_NAME.resolveModelAttribute(context, operation).asString() : null;

        Resource rootNode = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        ModelNode rootModel = rootNode.getModel();
        String profile = rootModel.hasDefined("profile-name") ? rootModel.get("profile-name").asString() : null;

        final ServiceTarget target = context.getServiceTarget();

        final ModuleIdentifier moduleId;
        final Module module;
        String slot = operation.hasDefined(MODULE_SLOT.getName()) ? MODULE_SLOT.resolveModelAttribute(context, operation).asString() : null;
        if (moduleName.contains(":")) {
            slot = moduleName.substring(moduleName.indexOf(":") + 1);
            moduleName = moduleName.substring(0, moduleName.indexOf(":"));
        } else {
            if (slot != null) {
                model.get(DRIVER_MODULE_NAME.getName()).set(moduleName + ":" + slot);
            }
        }

        try {
            moduleId = ModuleIdentifier.create(moduleName, slot);
            module = Module.getCallerModuleLoader().loadModule(moduleId);
        } catch (ModuleLoadException e) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToLoadModuleDriver(moduleName), e);
        }

        if (driverClassName == null) {
            final ServiceLoader<Driver> serviceLoader = module.loadService(Driver.class);
            boolean driverLoaded = false;
            if (serviceLoader != null) {
                for (Driver driver : serviceLoader) {
                    startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName, xaDataSourceClassName, profile);
                    driverLoaded = true;
                    //just consider first definition and create service for this. User can use different implementation only
                    // w/ explicit declaration of driver-class attribute
                    break;
                }
            }
            if (!driverLoaded)
                SUBSYSTEM_DATASOURCES_LOGGER.cannotFindDriverClassName(driverName);
        } else {
            try {
                final Class<? extends Driver> driverClass = module.getClassLoader().loadClass(driverClassName)
                        .asSubclass(Driver.class);
                final Constructor<? extends Driver> constructor = driverClass.getConstructor();
                final Driver driver = constructor.newInstance();
                startDriverServices(target, moduleId, driver, driverName, majorVersion, minorVersion, dataSourceClassName, xaDataSourceClassName, profile);
            } catch (Exception e) {
                SUBSYSTEM_DATASOURCES_LOGGER.cannotInstantiateDriverClass(driverClassName, e);
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.cannotInstantiateDriverClass(driverClassName));
            }
        }
    }

    public static void startDriverServices(final ServiceTarget target, final ModuleIdentifier moduleId, Driver driver, final String driverName, final Integer majorVersion,
                                           final Integer minorVersion, final String dataSourceClassName, final String xaDataSourceClassName,
                                           final String profile) throws IllegalStateException {
        final int majorVer = driver.getMajorVersion();
        final int minorVer = driver.getMinorVersion();
        if ((majorVersion != null && majorVersion != majorVer)
                || (minorVersion != null && minorVersion != minorVer)) {
            throw ConnectorLogger.ROOT_LOGGER.driverVersionMismatch();
        }

        final boolean compliant = driver.jdbcCompliant();
        if (compliant) {
            SUBSYSTEM_DATASOURCES_LOGGER.deployingCompliantJdbcDriver(driver.getClass(), majorVer, minorVer);
        } else {
            SUBSYSTEM_DATASOURCES_LOGGER.deployingNonCompliantJdbcDriver(driver.getClass(), majorVer, minorVer);
        }
        InstalledDriver driverMetadata = new InstalledDriver(driverName, moduleId, driver.getClass().getName(),
                dataSourceClassName, xaDataSourceClassName, majorVer, minorVer, compliant, profile);
        DriverService driverService = new DriverService(driverMetadata, driver);
        final ServiceBuilder<Driver> builder = target.addService(ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_")), driverService)
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        driverService.getDriverRegistryServiceInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);
        builder.install();
    }


}
