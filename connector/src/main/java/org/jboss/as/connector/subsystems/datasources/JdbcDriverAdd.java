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

import java.sql.Driver;
import java.util.ServiceLoader;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author John Bailey
 */
public class JdbcDriverAdd implements ModelAddOperationHandler {
    static final JdbcDriverAdd INSTANCE = new JdbcDriverAdd();

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources");

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);

        final String moduleName = operation.require(MODULE).asString();

        //Apply to the model
        final ModelNode model = context.getSubModel();
        model.get(NAME).set(pathAddress.getLastElement().getValue());
        model.get(MODULE).set(moduleName);

        // Compensating is remove
        final ModelNode compensating = Util.getResourceRemoveOperation(address);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget target = context.getServiceTarget();

                    final Module module;
                    try {
                        module = Module.getCallerModuleLoader().loadModule(ModuleIdentifier.create(moduleName));
                    } catch (ModuleLoadException e) {
                        throw new OperationFailedException(e, new ModelNode().set("Failed to load module for driver [" + moduleName + "]"));
                    }
                    final ServiceLoader<Driver> serviceLoader = module.loadService(Driver.class);
                    if (serviceLoader != null) for (Driver driver : serviceLoader) {

                        final int majorVersion = driver.getMajorVersion();
                        final int minorVersion = driver.getMinorVersion();
                        final boolean compliant = driver.jdbcCompliant();
                        if (compliant) {
                            log.infof("Deploying JDBC-compliant driver %s (version %d.%d)", driver.getClass(),
                                    Integer.valueOf(majorVersion), Integer.valueOf(minorVersion));
                        } else {
                            log.infof("Deploying non-JDBC-compliant driver %s (version %d.%d)", driver.getClass(),
                                    Integer.valueOf(majorVersion), Integer.valueOf(minorVersion));
                        }
                        target.addService(ServiceName.JBOSS.append("jdbc-driver", driver.getClass().getName(), Integer.toString(majorVersion), Integer.toString(minorVersion)),
                                new ValueService<Driver>(new ImmediateValue<Driver>(driver))).setInitialMode(ServiceController.Mode.ACTIVE)
                                .install();

                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }
}
