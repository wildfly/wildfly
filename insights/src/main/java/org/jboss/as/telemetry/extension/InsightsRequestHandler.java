package org.jboss.as.insights.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;

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

        final String rhnUid = operation.require(InsightsExtension.RHNUID).asString();
        final String rhnPw = operation.require(InsightsExtension.RHNPW).asString();

        if(operation.has(InsightsExtension.PROXY_PASSWORD) && operation.has(InsightsExtension.PROXY_USER)) {
            String proxyPwd = operation.get(InsightsExtension.PROXY_PASSWORD).asString();
            String proxyPort = operation.get(InsightsExtension.PROXY_PORT).asString();
            String proxyUrl = operation.get(InsightsExtension.PROXY_URL).asString();
            String proxyUser = operation.get(InsightsExtension.PROXY_USER).asString();
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
