/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.insights.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsRequestHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "enable";

    static final InsightsRequestHandler INSTANCE = new InsightsRequestHandler();

    static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, InsightsExtension.getResourceDescriptionResolver(null))
            .setRuntimeOnly()
            .addParameter(InsightsSubsystemDefinition.RHNUID)
            .addParameter(InsightsSubsystemDefinition.RHNPW)
            .addParameter(InsightsSubsystemDefinition.PROXYURL)
            .addParameter(InsightsSubsystemDefinition.PROXYPORT)
            .addParameter(InsightsSubsystemDefinition.PROXYPASSWORD)
            .addParameter(InsightsSubsystemDefinition.PROXYUSER)
            .build();

    private InsightsRequestHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String rhnUid = InsightsSubsystemDefinition.RHNUID.resolveModelAttribute(context, operation).asString();//operation.require(InsightsExtension.RHNUID).asString();
        final String rhnPw = InsightsSubsystemDefinition.RHNPW.resolveModelAttribute(context, operation).asString();//operation.require(InsightsExtension.RHNPW).asString();

        if(operation.has(InsightsExtension.PROXY_PASSWORD) && operation.has(InsightsExtension.PROXY_USER)) {
            String proxyPwd = InsightsSubsystemDefinition.PROXYPASSWORD.resolveModelAttribute(context, operation).asString();//operation.get(InsightsExtension.PROXY_PASSWORD).asString();
            String proxyPort = InsightsSubsystemDefinition.PROXYPORT.resolveModelAttribute(context, operation).asString();//operation.get(InsightsExtension.PROXY_PORT).asString();
            String proxyUrl = InsightsSubsystemDefinition.PROXYURL.resolveModelAttribute(context, operation).asString();//operation.get(InsightsExtension.PROXY_URL).asString();
            String proxyUser = InsightsSubsystemDefinition.PROXYUSER.resolveModelAttribute(context, operation).asString();//operation.get(InsightsExtension.PROXY_USER).asString();
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    InsightsService service = (InsightsService) context.getServiceRegistry(true).getRequiredService(InsightsService.createServiceName()).getValue();
                    service.setRhnLoginCredentials(rhnUid,rhnPw,proxyUrl,proxyPort,proxyUser,proxyPwd);
                    service.setEnabled(true);
                    context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
        else if(operation.has(InsightsExtension.PROXY_PORT) && operation.has(InsightsExtension.PROXY_URL)) {
            String proxyPort = operation.get(InsightsExtension.PROXY_PORT).asString();
            String proxyUrl = operation.get(InsightsExtension.PROXY_URL).asString();
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    InsightsService service = (InsightsService) context.getServiceRegistry(true).getRequiredService(InsightsService.createServiceName()).getValue();
                    service.setRhnLoginCredentials(rhnUid,rhnPw,proxyUrl,proxyPort);
                    service.setEnabled(true);
                    context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
        else {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    InsightsService service = (InsightsService) context.getServiceRegistry(true).getRequiredService(InsightsService.createServiceName()).getValue();
                    service.setRhnLoginCredentials(rhnUid,rhnPw);
                    service.setEnabled(true);
                    context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }
}
