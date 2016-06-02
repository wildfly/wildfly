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

package org.jboss.as.connector.deployers.ds.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYER_JDBC_LOGGER;

import java.lang.reflect.Constructor;
import java.sql.Driver;
import java.util.List;

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
            for (String driverClassName : driverNames) {
                try {
                    final Class<? extends Driver> driverClass = classLoader.loadClass(driverClassName).asSubclass(Driver.class);
                    final Constructor<? extends Driver> constructor = driverClass.getConstructor();
                    final Driver driver = constructor.newInstance();
                    final int majorVersion = driver.getMajorVersion();
                    final int minorVersion = driver.getMinorVersion();
                    final boolean compliant = driver.jdbcCompliant();
                    if (compliant) {
                        DEPLOYER_JDBC_LOGGER.deployingCompliantJdbcDriver(driverClass, Integer.valueOf(majorVersion),
                                Integer.valueOf(minorVersion));
                    } else {
                        DEPLOYER_JDBC_LOGGER.deployingNonCompliantJdbcDriver(driverClass, Integer.valueOf(majorVersion),
                                Integer.valueOf(minorVersion));
                    }
                    String driverName = deploymentUnit.getName();
                    if ((driverName.contains(".") && ! driverName.endsWith(".jar")) || driverNames.size() != 1) {
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

                } catch (Throwable e) {
                    DEPLOYER_JDBC_LOGGER.cannotInstantiateDriverClass(driverClassName, e);
                }

            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
