package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaCachedConnectionManagerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_CACHED_CONNECTION_MANAGER = PathElement.pathElement(CACHED_CONNECTION_MANAGER, CACHED_CONNECTION_MANAGER);
    static final JcaCachedConnectionManagerDefinition INSTANCE = new JcaCachedConnectionManagerDefinition();

    private JcaCachedConnectionManagerDefinition() {
        super(PATH_CACHED_CONNECTION_MANAGER,
                JcaExtension.getResourceDescriptionResolver(PATH_CACHED_CONNECTION_MANAGER.getKey()),
                CachedConnectionManagerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final CcmParameters parameter : CcmParameters.values()) {
            if (parameter != CcmParameters.INSTALL) {
                resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, JcaAttributeWriteHandler.INSTANCE);
            } else {
                resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, new ReloadRequiredWriteAttributeHandler());
            }
        }

    }

    public static enum CcmParameters {
        DEBUG(SimpleAttributeDefinitionBuilder.create("debug", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("debug")
                .build()),
        ERROR(SimpleAttributeDefinitionBuilder.create("error", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("error")
                .build()),
        INSTALL(SimpleAttributeDefinitionBuilder.create("install", ModelType.BOOLEAN)
                .setAllowExpression(false)
                .setAllowNull(true)
                .setDefaultValue(new ModelNode().set(false))
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .build());


        private CcmParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }


}
