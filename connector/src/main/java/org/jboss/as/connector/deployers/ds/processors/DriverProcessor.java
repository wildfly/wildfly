/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ds.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYER_JDBC_LOGGER;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Enumeration;
import java.util.List;

import org.jboss.as.connector._drivermanager.DriverManagerAdapter;
import org.jboss.as.connector.services.driver.DriverService;
import org.jboss.as.connector.services.driver.InstalledDriver;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.Assert;

/**
 * Deploy any JDBC drivers in a deployment unit.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DriverProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} */
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final ServicesAttachment servicesAttachment = deploymentUnit.getAttachment(Attachments.SERVICES);
        if (module != null && servicesAttachment != null) {
            final ModuleClassLoader classLoader = module.getClassLoader();
            final List<String> driverNames = servicesAttachment.getServiceImplementations(Driver.class.getName());
            int idx = 0;
            for (String driverClassName : driverNames) {
                try {
                    final Class<? extends Driver> driverClass = classLoader.loadClass(driverClassName).asSubclass(Driver.class);
                    final Constructor<? extends Driver> constructor = driverClass.getConstructor();
                    final Driver driver = constructor.newInstance();
                    final int majorVersion = driver.getMajorVersion();
                    final int minorVersion = driver.getMinorVersion();
                    final boolean compliant = driver.jdbcCompliant();
                    if (compliant) {
                        DEPLOYER_JDBC_LOGGER.deployingCompliantJdbcDriver(driverClass, majorVersion, minorVersion);
                    } else {
                        DEPLOYER_JDBC_LOGGER.deployingNonCompliantJdbcDriver(driverClass, majorVersion, minorVersion);
                    }
                    final String deploymentName = deploymentUnit.getName();
                    String driverName = deploymentName;
                    // in case jdbc drivers are placed in war/ear archives
                    boolean driverWrapped = deploymentName.contains(".") && ! deploymentName.endsWith(".jar");
                    if (driverWrapped || driverNames.size() != 1) {
                        driverName += "_" + driverClassName + "_" + majorVersion + "_" + minorVersion;
                    }
                    InstalledDriver driverMetadata = new InstalledDriver(driverName, driverClass.getName(), null, null, majorVersion,
                            minorVersion, compliant);
                    DriverService driverService = new DriverService(driverMetadata, driver);
                    phaseContext
                            .getServiceTarget()
                            .addService(ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_")), driverService)
                            .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                                    driverService.getDriverRegistryServiceInjector()).setInitialMode(Mode.ACTIVE).install();

                    if (idx == 0 && driverNames.size() != 1 && !driverWrapped) {
                        // create short name driver service
                        driverMetadata = new InstalledDriver(deploymentName, driverClass.getName(), null,
                                null, majorVersion, minorVersion, compliant);
                        driverService = new DriverService(driverMetadata, driver);
                        phaseContext.getServiceTarget()
                                .addService(ServiceName.JBOSS.append("jdbc-driver", deploymentName.replaceAll("\\.", "_")), driverService)
                                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class, driverService.getDriverRegistryServiceInjector())
                                .setInitialMode(Mode.ACTIVE).install();
                    }
                    idx++;
                } catch (Throwable e) {
                    DEPLOYER_JDBC_LOGGER.cannotInstantiateDriverClass(driverClassName, e);
                }

            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(final DeploymentUnit context) {
        /**
         * https://issues.redhat.com/browse/WFLY-14114
         *
         * This hack allows to deregister all drivers registered by this module. See comments in {@link DriverManagerAdapterProcessor}
         */
        final Module module = context.getAttachment(Attachments.MODULE);
        final ServicesAttachment servicesAttachment = context.getAttachment(Attachments.SERVICES);
        if (module != null && servicesAttachment != null) {
            final List<String> driverNames = servicesAttachment.getServiceImplementations(Driver.class.getName());
            if (!driverNames.isEmpty()) {
                try {
                    Class<?> driverManagerAdapterClass = module.getClassLoader().loadClass(DriverManagerAdapter.class.getName());

                    Method getDriversMethod = driverManagerAdapterClass.getDeclaredMethod("getDrivers");
                    Enumeration<Driver> drivers = (Enumeration<Driver>) getDriversMethod.invoke(null, null);

                    Method deregisterDriverMethod = driverManagerAdapterClass.getDeclaredMethod("deregisterDriver", Driver.class);
                    while (drivers.hasMoreElements()) {
                        Driver driver = drivers.nextElement();
                        if(driverNames.contains(driver.getClass().getName())) {
                            deregisterDriverMethod.invoke(null, driver);
                        }
                    }
                } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    Assert.unreachableCode();
                }
            }
        }
    }
}
