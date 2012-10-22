package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.connector.subsystems.jca.Constants.BEAN_VALIDATION;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author Stefano Maestri
 */
public class JcaBeanValidationDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_BEAN_VALIDATION = PathElement.pathElement(BEAN_VALIDATION, BEAN_VALIDATION);
    static final JcaBeanValidationDefinition INSTANCE = new JcaBeanValidationDefinition();

    private JcaBeanValidationDefinition() {
        super(PATH_BEAN_VALIDATION,
                JcaExtension.getResourceDescriptionResolver(PATH_BEAN_VALIDATION.getKey()),
                BeanValidationAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final BeanValidationParameters parameter : BeanValidationParameters.values()) {
            resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, JcaAttributeWriteHandler.INSTANCE);
        }

    }

    public static enum BeanValidationParameters {
        BEAN_VALIDATION_ENABLED(SimpleAttributeDefinitionBuilder.create("enabled", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(true))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("enabled")
                .build());

        private BeanValidationParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

}
