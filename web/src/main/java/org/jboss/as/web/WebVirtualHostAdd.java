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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;

import java.util.Locale;

/**
 * {@code OperationHandler} responsible for adding a virtual host.
 *
 * @author Emanuel Muckenhuber
 */
class WebVirtualHostAdd implements ModelAddOperationHandler, DescriptionProvider {

    static final WebVirtualHostAdd INSTANCE = new WebVirtualHostAdd();
    private static final String DEFAULT_RELATIVE_TO = "jboss.server.log.dir";
    private static final String TEMP_DIR = "jboss.server.temp.dir";
    private static final String[] NO_ALIASES = new String[0];

    private WebVirtualHostAdd() {
        //
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        final ModelNode subModel = context.getSubModel();
        subModel.get(Constants.ALIAS).set(operation.get(Constants.ALIAS));
        subModel.get(Constants.ACCESS_LOG).set(operation.get(Constants.ACCESS_LOG));
        subModel.get(Constants.REWRITE).set(operation.get(Constants.REWRITE));

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();
                    final WebVirtualHostService service = new WebVirtualHostService(name, aliases(operation));
                    final ServiceBuilder<?> serviceBuilder = serviceTarget.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(name), service)
                            .addDependency(AbstractPathService.pathNameOf(TEMP_DIR), String.class, service.getTempPathInjector())
                            .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, service.getWebServer());
                    if (operation.hasDefined(Constants.ACCESS_LOG)) {
                        final ModelNode accessLog = operation.get(Constants.ACCESS_LOG);
                        service.setAccessLog(accessLog.clone());
                        // Create the access log service
                        accessLogService(name, accessLog, serviceTarget);
                        serviceBuilder.addDependency(WebSubsystemServices.JBOSS_WEB_HOST.append(name, Constants.ACCESS_LOG), String.class, service.getAccessLogPathInjector());
                    }
                    if (operation.hasDefined(Constants.REWRITE)) {
                        service.setRewrite(operation.get(Constants.REWRITE).clone());
                    }
                    if (operation.hasDefined(Constants.DEFAULT_WEB_MODULE)) service.setDefaultWebModule(operation.get(Constants.DEFAULT_WEB_MODULE).asString());
                    serviceBuilder.addListener(new ResultHandler.ServiceStartListener(resultHandler));
                    serviceBuilder.install();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }

    static String[] aliases(final ModelNode node) {
        if(node.has(Constants.ALIAS)) {
            final ModelNode aliases = node.require(Constants.ALIAS);
            final int size = aliases.asInt();
            final String[] array = new String[size];
            for(int i = 0; i < size; i ++) array[i] = aliases.get(i).asString();
            return array;
        }
        return NO_ALIASES;
    }

    static void accessLogService(final String hostName, final ModelNode element, final ServiceTarget target) {
        if(element.has(Constants.ACCESS_LOG)) {
            final ModelNode accessLog = element.get(Constants.ACCESS_LOG);
            final String relativeTo = accessLog.has(RELATIVE_TO) ? accessLog.get(RELATIVE_TO).asString() : DEFAULT_RELATIVE_TO;
            final String path = accessLog.has(PATH) ? accessLog.get(PATH).asString() : hostName;
            RelativePathService.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName, Constants.ACCESS_LOG),
                    path, relativeTo, target);
        } else {
            RelativePathService.addService(WebSubsystemServices.JBOSS_WEB_HOST.append(hostName, Constants.ACCESS_LOG),
                    hostName, DEFAULT_RELATIVE_TO, target);
        }
    }

    static ModelNode getAddOperation(final ModelNode address, final ModelNode subModel) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        if(subModel.hasDefined(Constants.ALIAS)) {
            operation.get(Constants.ALIAS).set(subModel.get(Constants.ALIAS));
        }
        if(subModel.hasDefined(Constants.ACCESS_LOG)) {
            operation.get(Constants.ACCESS_LOG).set(subModel.get(Constants.ACCESS_LOG));
        }
        if(subModel.hasDefined(Constants.REWRITE)) {
            operation.get(Constants.REWRITE).set(subModel.get(Constants.REWRITE));
        }
        return operation;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return WebSubsystemDescriptions.getVirtualServerAdd(locale);
    }
}
