/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.web.WebExtension.FILE_PATH;
import static org.jboss.as.web.Constants.PARAM;
import java.util.List;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;

/**
 * {@code OperationHandler} responsible for adding a web valve.
 *
 * @author Jean-Frederic Clere
 */
class WebValveAdd extends AbstractAddStepHandler {

    static final WebValveAdd INSTANCE = new WebValveAdd();

    private WebValveAdd() {
        //
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        model.get(WebValveDefinition.NAME.getName()).set(address.getLastElement().getValue());

        for (SimpleAttributeDefinition def : WebValveDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
        WebValveDefinition.PARAMS.validateAndSet(operation,model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode baseOperation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        ModelNode operation = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final PathAddress address = PathAddress.pathAddress(baseOperation.get(OP_ADDR));

        final String name = address.getLastElement().getValue();
        String classname = null;
        if (WebValveDefinition.CLASS_NAME.resolveModelAttribute(context, operation).isDefined())
            classname = WebValveDefinition.CLASS_NAME.resolveModelAttribute(context, operation).asString();
        String module = null;
        if (WebValveDefinition.MODULE.resolveModelAttribute(context, operation).isDefined())
            module = WebValveDefinition.MODULE.resolveModelAttribute(context, operation).asString();

        final boolean enabled = WebValveDefinition.ENABLED.resolveModelAttribute(context, operation).asBoolean();
        final WebValveService service = new WebValveService(name, classname, module);
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB_VALVE.append(name), service)
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManagerInjector())
                .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer());

        if (operation.get(FILE_PATH.getKey(), FILE_PATH.getValue()).isDefined()) {
            final ModelNode directory = operation.get(FILE_PATH.getKey(), FILE_PATH.getValue());
            String relativeTo = null;
            if (WebValveFileDefinition.RELATIVE_TO.resolveModelAttribute(context, directory).isDefined())
                relativeTo = WebValveFileDefinition.RELATIVE_TO.resolveModelAttribute(context, directory).asString();
            String path = null;
            if (WebValveFileDefinition.PATH.resolveModelAttribute(context, directory).isDefined())
                path = WebValveFileDefinition.PATH.resolveModelAttribute(context, directory).asString();
            service.setFilePaths(path, relativeTo);
        }

        if (operation.hasDefined(PARAM)) {
            service.setParam(operation.get(PARAM).clone());
        }

        serviceBuilder.setInitialMode(enabled ? Mode.ACTIVE : Mode.NEVER);
        if (enabled)
            serviceBuilder.addListener(verificationHandler);
        newControllers.add(serviceBuilder.install());
    }
}
