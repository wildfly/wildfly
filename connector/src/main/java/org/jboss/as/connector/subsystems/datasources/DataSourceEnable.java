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

import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation handler responsible for enabling an existing data-source.
 *
 * @author John Bailey
 */
public class DataSourceEnable implements NewStepHandler {
    static final DataSourceEnable LOCAL_INSTANCE = new DataSourceEnable(false);
    static final DataSourceEnable XA_INSTANCE = new DataSourceEnable(true);

    private final boolean xa;

    public DataSourceEnable(boolean xa) {
        super();
        this.xa = xa;
    }

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources");

    public void execute(NewOperationContext context, ModelNode operation) {
        context.readModelForUpdate(PathAddress.EMPTY_ADDRESS).get(ENABLED).set(true);

        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {

                    final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                    final String rawJndiName = model.require(JNDINAME).asString();
                    final String jndiName;
                    if (!rawJndiName.startsWith("java:/") && model.hasDefined(USE_JAVA_CONTEXT) && model.get(USE_JAVA_CONTEXT).asBoolean()) {
                        jndiName = "java:/" + rawJndiName;
                    } else {
                        jndiName = rawJndiName;
                    }

                    final ServiceRegistry registry = context.getServiceRegistry(true);

                    if (isXa()) {
                        final ServiceName dataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE
                                .append(jndiName);
                        final ServiceController<?> dataSourceConfigController = registry
                                .getService(dataSourceConfigServiceName);
                        if (dataSourceConfigController != null) {
                            ((XaDataSource) dataSourceConfigController.getValue()).setEnabled(true);

                        }
                    } else {
                        final ServiceName dataSourceConfigServiceName = DataSourceConfigService.SERVICE_NAME_BASE
                                .append(jndiName);
                        final ServiceController<?> dataSourceConfigController = registry
                                .getService(dataSourceConfigServiceName);
                        if (dataSourceConfigController != null) {
                            ((DataSource) dataSourceConfigController.getValue()).setEnabled(true);

                        }
                    }

                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);
                    if (dataSourceController != null) {
                        if (!ServiceController.State.UP.equals(dataSourceController.getState())) {
                            dataSourceController.setMode(ServiceController.Mode.ACTIVE);
                        } else {
                            throw new OperationFailedException(new ModelNode().set("Data-source service [" + jndiName + "] is already started"));
                        }
                    } else {
                        throw new OperationFailedException(new ModelNode().set("Data-source service [" + jndiName + "] is not available"));
                    }

                    final ServiceName referenceServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceController<?> referenceController = registry.getService(referenceServiceName);
                    if (referenceController != null && !ServiceController.State.UP.equals(referenceController.getState())) {
                        referenceController.setMode(ServiceController.Mode.ACTIVE);
                    }

                    final ServiceName binderServiceName = ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName);
                    final ServiceController<?> binderController = registry.getService(binderServiceName);
                    if (binderController != null && !ServiceController.State.UP.equals(binderController.getState())) {
                        binderController.setMode(ServiceController.Mode.ACTIVE);
                    }
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    public static DataSourceEnable getLocalInstance() {
        return LOCAL_INSTANCE;
    }

    public boolean isXa() {
        return xa;
    }
}
