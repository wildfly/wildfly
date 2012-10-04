package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 21:04
 */
class MailSessionDefinition extends SimpleResourceDefinition {
    public static MailSessionDefinition INSTANCE = new MailSessionDefinition();

    private MailSessionDefinition() {
        super(MailExtension.MAIL_SESSION_PATH,
                MailExtension.getResourceDescriptionResolver(MailSubsystemModel.MAIL_SESSION),
                MailSessionAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    protected static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.JNDI_NAME, ModelType.STRING, false)
                    .setAllowExpression(true)
                    .setRestartAllServices()
                    .build();
    protected static final SimpleAttributeDefinition FROM =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.FROM, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .build();
    protected static final SimpleAttributeDefinition DEBUG =
            new SimpleAttributeDefinitionBuilder(MailSubsystemModel.DEBUG, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .setRestartAllServices()
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = {DEBUG, JNDI_NAME, FROM};

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            rootResourceRegistration.registerReadWriteAttribute(attr, null, ReloadRequiredRemoveStepHandler.INSTANCE);
        }
    }

    private static class SessionAttributeWriteHandler extends AbstractWriteAttributeHandler {
        protected static SessionAttributeWriteHandler INSTANCE = new SessionAttributeWriteHandler();

        private SessionAttributeWriteHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder handbackHolder) throws OperationFailedException {
            String jndiName = JNDI_NAME.resolveModelAttribute(context, context.readResource(PathAddress.EMPTY_ADDRESS).getModel()).asString();
            final ServiceName serviceName = MailSessionAdd.SERVICE_NAME_BASE.append(jndiName);
            AttributeDefinition def = getAttributeDefinition(attributeName);
            ServiceController svcCtrl = context.getServiceRegistry(false).getService(serviceName);
            context.removeService(svcCtrl);
            MailSessionService service = (MailSessionService) svcCtrl.getService();
            if (def == DEBUG) {
                service.getConfig().setDebug(resolvedValue.asBoolean());
            } else if (def == FROM) {
                service.getConfig().setFrom(resolvedValue.asString());
            }
            //context.getServiceTarget().addService(serviceName);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {

        }
    }


}
