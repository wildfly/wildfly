package org.jboss.as.server.deploymentoverlay;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

/**
 * @author Stuart Douglas
 */
public class ContentDefinition extends SimpleResourceDefinition {

    static final ContentAttributeDefinition CONTENT =
            new ContentAttributeDefinition(ModelDescriptionConstants.CONTENT, ModelType.BYTES, false);

    private final ContentRepository contentRepository;
    private final SimpleOperationDefinition readContent;
    private static final AttributeDefinition[] ATTRIBUTES = {CONTENT};

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public ContentDefinition(final ContentRepository contentRepository, final DeploymentFileRepository remoteRepository) {
        super(DeploymentOverlayModel.CONTENT_PATH,
                ControllerResolver.getResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY,ModelDescriptionConstants.CONTENT),
                new ContentAdd(contentRepository, remoteRepository),
                ContentRemove.INSTANCE);
        this.contentRepository = contentRepository;
        readContent = new SimpleOperationDefinition(READ_CONTENT, getResourceDescriptionResolver());
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        ReadContentHandler handler = new ReadContentHandler(contentRepository);
        resourceRegistration.registerOperationHandler(readContent, handler);
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
            result.get(TYPE).set(ModelType.OBJECT);
            valueType.get(INPUT_STREAM_INDEX, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, INPUT_STREAM_INDEX));
            valueType.get(HASH, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, HASH));
            valueType.get(BYTES, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, BYTES));
            valueType.get(URL, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, URL));
        }

        private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
            final ModelNode valueType = parent.get(VALUE_TYPE);
            final ModelNode inputStreamIndex = valueType.get(INPUT_STREAM_INDEX);
            inputStreamIndex.get(TYPE).set(ModelType.INT);
            inputStreamIndex.get(DESCRIPTION);
            inputStreamIndex.get(REQUIRED).set(false);
            inputStreamIndex.get(MIN).set(0);
            inputStreamIndex.get(NILLABLE).set(true);

            final ModelNode hash = valueType.get(HASH);
            hash.get(TYPE).set(ModelType.BYTES);
            hash.get(DESCRIPTION);
            hash.get(REQUIRED).set(false);
            hash.get(MIN_LENGTH).set(20);
            hash.get(MAX_LENGTH).set(20);
            hash.get(NILLABLE).set(true);

            final ModelNode bytes = valueType.get(BYTES);
            bytes.get(TYPE).set(ModelType.BYTES);
            bytes.get(DESCRIPTION);
            bytes.get(REQUIRED).set(false);
            bytes.get(MIN_LENGTH).set(1);
            bytes.get(NILLABLE).set(true);

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
