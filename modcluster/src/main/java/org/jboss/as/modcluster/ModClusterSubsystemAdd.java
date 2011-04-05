/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.modcluster;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * The managed subsystem add update.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterSubsystemAdd implements ModelAddOperationHandler, DescriptionProvider {

    static final ModClusterSubsystemAdd INSTANCE = new ModClusterSubsystemAdd();

    @Override
    public  OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        compensatingOperation.get(ModelDescriptionConstants.OP_ADDR).set(operation.require(ModelDescriptionConstants.OP_ADDR));

        final ModelNode config = operation.get(CommonAttributes.MOD_CLUSTER_CONFIG);

        context.getSubModel().set(config);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    // Add mod_cluster service
                    final ModClusterService service = new ModClusterService(config.clone());
                    context.getServiceTarget().addService(ModClusterService.NAME, service)
                        .addListener(new ResultHandler.ServiceStartListener(resultHandler))
                        .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer())
                        .setInitialMode(Mode.ACTIVE)
                        .install();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ModClusterSubsystemDescriptions.getSubsystemAddDescription(locale);
    }

}
