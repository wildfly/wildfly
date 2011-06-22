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

import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler responsible for removing a jdbc driver.
 *
 * @author John Bailey
 */
public class JdbcDriverRemove extends AbstractRemoveStepHandler {
    static final JdbcDriverRemove INSTANCE = new JdbcDriverRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String driverName = model.get(DRIVER_NAME).asString();
        final String moduleName = operation.require(DRIVER_MODULE_NAME).asString();

        // Use the module for now.  Would be nice to keep the driver info in the model.
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

            context.removeService(ServiceName.JBOSS.append("jdbc-driver", driver.getClass().getName(), Integer.toString(majorVersion), Integer.toString(minorVersion)));
        }

    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
