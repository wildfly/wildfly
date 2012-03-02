package org.jboss.as.web;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:03
 */
public class WebContainerDefinition extends SimpleResourceDefinition {
    public static final WebContainerDefinition INSTANCE = new WebContainerDefinition();

    protected static final SimpleListAttributeDefinition WELCOME_FILES =
            SimpleListAttributeDefinition.Builder.of(Constants.WELCOME_FILE,
                    new SimpleAttributeDefinitionBuilder(Constants.WELCOME_FILE, ModelType.STRING, true)
                            .setXmlName(Constants.WELCOME_FILE)
                            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                            .setValidator(new StringLengthValidator(1, true))
                            .build())
                    .setAllowNull(true)
                    .build();
    /*private static final SimpleAttributeDefinition MIME_MAPPING =
            new SimpleAttributeDefinitionBuilder(Constants.MIME_MAPPING, ModelType.STRING, true)
                    .setXmlName(Constants.MIME_MAPPING)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();*/
    protected static final PropertiesAttributeDefinition MIME_MAPPINGS = new PropertiesAttributeDefinition(Constants.MIME_MAPPING, Constants.MIME_MAPPING, true);

    private static final SimpleAttributeDefinition MIME_NAME = new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    private static final SimpleAttributeDefinition MIME_VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    protected static final AttributeDefinition[] CONTAINER_ATTRIBUTES={
            WELCOME_FILES,
            MIME_MAPPINGS,
    };

    private WebContainerDefinition() {
        super(WebExtension.CONTAINER_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.CONTAINER),
                WebContainerAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }


    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration container) {
        super.registerOperations(container);
        container.registerOperationHandler("add-mime",
                MimeMappingAdd.INSTANCE,
                new DefaultOperationDescriptionProvider("add-mime", WebExtension.getResourceDescriptionResolver("container.mime-mapping"), MIME_NAME, MIME_VALUE));
        container.registerOperationHandler("remove-mime",
                MimeMappingRemove.INSTANCE,
                new DefaultOperationDescriptionProvider("remove-mime", WebExtension.getResourceDescriptionResolver("container.mime-mapping"), MIME_NAME));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration container) {
        container.registerReadWriteAttribute(WELCOME_FILES, null, new ReloadRequiredWriteAttributeHandler(WELCOME_FILES));
        container.registerReadWriteAttribute(MIME_MAPPINGS, null, new ReloadRequiredWriteAttributeHandler(MIME_MAPPINGS));
    }

    protected static class PropertiesAttributeDefinition extends MapAttributeDefinition {

        public PropertiesAttributeDefinition(final String name, final String xmlName, boolean allowNull) {
            super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new ModelTypeValidator(ModelType.STRING), null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
        }

        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            if (!isMarshallable(resourceModel)) { return; }

            resourceModel = resourceModel.get(getName());
            writer.writeStartElement(getName());
            for (ModelNode property : resourceModel.asList()) {
                writer.writeEmptyElement(getXmlName());
                writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), property.asProperty().getName());
                writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName(), property.asProperty().getValue().asString());
            }
            writer.writeEndElement();
        }
    }
}
