package org.jboss.as.server.deploymentoverlay;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

/**
 * @author Stuart Douglas
 */
public class ContentDefinition extends SimpleResourceDefinition {

    static final ContentAttributeDefinition CONTENT =
            new ContentAttributeDefinition(ModelDescriptionConstants.CONTENT, ModelType.OBJECT, false);

    private static final AttributeDefinition[] ATTRIBUTES = {CONTENT};

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public ContentDefinition(final ContentRepository contentRepository, final DeploymentFileRepository remoteRepository) {
        super(DeploymentOverlayModel.CONTENT_PATH,
                CommonDescriptions.getResourceDescriptionResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY + "." + ModelDescriptionConstants.CONTENT, false),
                new ContentAdd(contentRepository, remoteRepository),
                ContentRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }

    public static final class ContentAttributeDefinition extends SimpleAttributeDefinition {


        private ContentAttributeDefinition(final String name, final ModelType type, final boolean allowNull) {
            super(name, type, allowNull);
        }

        @Override
        public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName,
                                                          ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
            addOperationParameterValueTypeDescription(result, operationName, resolver, locale, bundle);
            return result;
        }


        private void addOperationParameterValueTypeDescription(ModelNode result, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            final ModelNode valueType = getNoTextValueTypeDescription(result);
            valueType.get(INPUT_STREAM_INDEX, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, INPUT_STREAM_INDEX));
            valueType.get(HASH, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, HASH));
            valueType.get(BYTES, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, BYTES));
            valueType.get(URL, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, URL));
        }

        private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
            final ModelNode valueType = parent.get(VALUE_TYPE);
            final ModelNode inputStreamIndex = valueType.get(INPUT_STREAM_INDEX);
            inputStreamIndex.get(TYPE).set(ModelType.INT);
            inputStreamIndex.get(INPUT_STREAM_INDEX, DESCRIPTION);
            inputStreamIndex.get(INPUT_STREAM_INDEX, REQUIRED).set(false);
            inputStreamIndex.get(INPUT_STREAM_INDEX, MIN).set(0);
            inputStreamIndex.get(INPUT_STREAM_INDEX, NILLABLE).set(true);

            final ModelNode hash = valueType.get(HASH);
            hash.get(TYPE).set(ModelType.BYTES);
            hash.get(HASH, DESCRIPTION);
            hash.get(HASH, REQUIRED).set(false);
            hash.get(HASH, MIN_LENGTH).set(20);
            hash.get(HASH, MAX_LENGTH).set(20);
            hash.get(HASH, NILLABLE).set(true);

            final ModelNode bytes = valueType.get(BYTES);
            bytes.get(TYPE).set(ModelType.BYTES);
            bytes.get(BYTES, DESCRIPTION);
            bytes.get(BYTES, REQUIRED).set(false);
            bytes.get(BYTES, MIN_LENGTH).set(1);
            bytes.get(BYTES, NILLABLE).set(true);

            final ModelNode url = valueType.get(URL);
            url.get(TYPE).set(ModelType.STRING);
            url.get(DESCRIPTION);
            url.get(REQUIRED).set(false);
            url.get(MIN_LENGTH).set(1);
            url.get(NILLABLE).set(true);

            return valueType;
        }


    }
}
