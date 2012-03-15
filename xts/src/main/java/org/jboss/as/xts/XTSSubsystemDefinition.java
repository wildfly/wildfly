package org.jboss.as.xts;

import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class XTSSubsystemDefinition extends SimpleResourceDefinition {
    protected static final XTSSubsystemDefinition INSTANCE = new XTSSubsystemDefinition();

    protected static final SimpleAttributeDefinition ENVIRONMENT_URL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URL, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(Attribute.URL.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                            //.setDefaultValue(new ModelNode().setExpression("http://${jboss.bind.address:127.0.0.1}:8080/ws-c11/ActivationService"))
                    .build();

    @Deprecated //just legacy support
    private static final ObjectTypeAttributeDefinition ENVIRONMENT = ObjectTypeAttributeDefinition.
            Builder.of(CommonAttributes.XTS_ENVIRONMENT, ENVIRONMENT_URL)
            .build();


    private XTSSubsystemDefinition() {
        super(XTSExtension.SUBSYSTEM_PATH,
                XTSExtension.getResourceDescriptionResolver(null),
                XTSSubsystemAdd.INSTANCE,
                XTSSubsystemRemove.INSTANCE);
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(ENVIRONMENT_URL, null, new ReloadRequiredWriteAttributeHandler(ENVIRONMENT_URL));
        //this here just for legacy support!
        resourceRegistration.registerReadOnlyAttribute(ENVIRONMENT, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode url = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get(ModelDescriptionConstants.URL);
                context.getResult().get(ModelDescriptionConstants.URL).set(url);
                context.completeStep();
            }
        });
    }
}
