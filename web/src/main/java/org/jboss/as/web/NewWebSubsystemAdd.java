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

package org.jboss.as.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import javax.management.MBeanServer;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.NewBootOperationContext;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.web.deployment.JBossWebParsingDeploymentProcessor;
import org.jboss.as.web.deployment.ServletContainerInitializerDeploymentProcessor;
import org.jboss.as.web.deployment.TldParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationDeploymentProcessor;
import org.jboss.as.web.deployment.WarClassloadingDependencyProcessor;
import org.jboss.as.web.deployment.WarDeploymentInitializingProcessor;
import org.jboss.as.web.deployment.WarDeploymentProcessor;
import org.jboss.as.web.deployment.WarMetaDataProcessor;
import org.jboss.as.web.deployment.WarStructureDeploymentProcessor;
import org.jboss.as.web.deployment.WebFragmentParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WebParsingDeploymentProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author Emanuel Muckenhuber
 */
class NewWebSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    static final NewWebSubsystemAdd INSTANCE = new NewWebSubsystemAdd();
    private static final String DEFAULT_HOST = "localhost";
    private static final String TEMP_DIR = "jboss.server.temp.dir";

    private NewWebSubsystemAdd() {
        //
    }

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext updateContext, ModelNode operation, ResultHandler resultHandler) {
        final ModelNode config = operation.get(CommonAttributes.CONTAINER_CONFIG);

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        if(updateContext instanceof NewBootOperationContext) {
            final NewBootOperationContext ctx = (NewBootOperationContext) updateContext;

            final String defaultHost = operation.has(CommonAttributes.DEFAULT_HOST) ?
                    operation.get(CommonAttributes.DEFAULT_HOST).asString() : DEFAULT_HOST;

            try {
                final WebServerService service = new WebServerService(defaultHost);
                ctx.getServiceTarget().addService(WebSubsystemElement.JBOSS_WEB, service)
                    .addDependency(AbstractPathService.pathNameOf(TEMP_DIR), String.class, service.getPathInjector())
                    .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, service.getMbeanServer())
                    .setInitialMode(Mode.ON_DEMAND)
                    .install();
            } catch (Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }

            final NewSharedWebMetaDataBuilder sharedWebBuilder = new NewSharedWebMetaDataBuilder(config.clone());
            final NewSharedTldsMetaDataBuilder sharedTldsBuilder = new NewSharedTldsMetaDataBuilder(config.clone());

            ctx.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_WAR_DEPLOYMENT_INIT, new WarDeploymentInitializingProcessor());
            ctx.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_WAR, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder.create()));
            ctx.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT, new WebParsingDeploymentProcessor());
            ctx.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT_FRAGMENT, new WebFragmentParsingDeploymentProcessor());
            ctx.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_JBOSS_WEB_DEPLOYMENT, new JBossWebParsingDeploymentProcessor());
            ctx.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TLD_DEPLOYMENT, new TldParsingDeploymentProcessor());
            ctx.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WAR_MODULE, new WarClassloadingDependencyProcessor());
            ctx.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_ANNOTATION_WAR, new WarAnnotationDeploymentProcessor());
            ctx.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_SERVLET_INIT_DEPLOYMENT, new ServletContainerInitializerDeploymentProcessor());
            ctx.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WAR_METADATA, new WarMetaDataProcessor());
            ctx.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WAR_DEPLOYMENT, new WarDeploymentProcessor(defaultHost));
        }

        final ModelNode subModel = updateContext.getSubModel();
        subModel.get(CommonAttributes.CONTAINER_CONFIG).set(config);
        subModel.get(CommonAttributes.CONNECTOR).setEmptyObject();
        subModel.get(CommonAttributes.VIRTUAL_SERVER).setEmptyObject();

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
