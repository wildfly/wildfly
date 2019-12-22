package org.jboss.as.jaxrs;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Jaxrs configuration attributes.
 *
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public interface  JaxrsAttribute {

    public static final String RESTEASY_PARAMETER_GROUP = "resteasy-parameter-group";

    SimpleAttributeDefinition JAXRS_2_0_REQUEST_MATCHING = new SimpleAttributeDefinitionBuilder(JaxrsConstants.JAXRS_2_0_REQUEST_MATCHING, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_ADD_CHARSET = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_ADD_CHARSET, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_BUFFER_EXCEPTION_ENTITY = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_BUFFER_EXCEPTION_ENTITY, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_DISABLE_HTML_SANITIZER = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DISABLE_HTML_SANITIZER, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_DISABLE_PROVIDERS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DISABLE_PROVIDERS, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JaxrsValidator.LIST_VALIDATOR_INSTANCE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_GZIP_MAX_INPUT = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_GZIP_MAX_INPUT, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(10000000))
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_JNDI_RESOURCES = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_JNDI_RESOURCES, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JaxrsValidator.MAP_VALIDATOR_INSTANCE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_LANGUAGE_MAPPINGS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_LANGUAGE_MAPPINGS, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JaxrsValidator.MAP_VALIDATOR_INSTANCE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_MEDIA_TYPE_MAPPINGS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_MEDIA_TYPE_MAPPINGS, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JaxrsValidator.MAP_VALIDATOR_INSTANCE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_MEDIA_TYPE_PARAM_MAPPING = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_MEDIA_TYPE_PARAM_MAPPING, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_PROVIDERS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_PROVIDERS, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JaxrsValidator.LIST_VALIDATOR_INSTANCE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_RESOURCES = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_RESOURCES, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(JaxrsValidator.LIST_VALIDATOR_INSTANCE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_RFC7232_PRECONDITIONS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_RFC7232_PRECONDITIONS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_ROLE_BASED_SECURITY = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_ROLE_BASED_SECURITY, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_SECURE_RANDOM_MAX_USE = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_SECURE_RANDOM_MAX_USE, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.INT, false))
            .setDefaultValue(new ModelNode(100))
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_USE_BUILTIN_PROVIDERS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_USE_BUILTIN_PROVIDERS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_USE_CONTAINER_FORM_PARAMS = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_USE_CONTAINER_FORM_PARAMS, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    SimpleAttributeDefinition RESTEASY_WIDER_REQUEST_MATCHING = new SimpleAttributeDefinitionBuilder(JaxrsConstants.RESTEASY_WIDER_REQUEST_MATCHING, ModelType.BOOLEAN)
            .setRequired(false)
            .setAllowExpression(true)
            .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, false))
            .setDefaultValue(ModelNode.FALSE)
            .setAttributeGroup(RESTEASY_PARAMETER_GROUP)
            .build();

    public static SimpleAttributeDefinition[] attributes = new SimpleAttributeDefinition[] {
            JAXRS_2_0_REQUEST_MATCHING,
            RESTEASY_ADD_CHARSET,
            RESTEASY_BUFFER_EXCEPTION_ENTITY,
            RESTEASY_DISABLE_HTML_SANITIZER,
            RESTEASY_DISABLE_PROVIDERS,
            RESTEASY_DOCUMENT_EXPAND_ENTITY_REFERENCES,
            RESTEASY_DOCUMENT_SECURE_DISABLE_DTDS,
            RESTEASY_DOCUMENT_SECURE_PROCESSING_FEATURE,
            RESTEASY_GZIP_MAX_INPUT,
            RESTEASY_JNDI_RESOURCES,
            RESTEASY_LANGUAGE_MAPPINGS,
            RESTEASY_MEDIA_TYPE_MAPPINGS,
            RESTEASY_MEDIA_TYPE_PARAM_MAPPING,
            RESTEASY_PROVIDERS,
            RESTEASY_RESOURCES,
            RESTEASY_RFC7232_PRECONDITIONS,
            RESTEASY_ROLE_BASED_SECURITY,
            RESTEASY_SECURE_RANDOM_MAX_USE,
            RESTEASY_USE_BUILTIN_PROVIDERS,
            RESTEASY_USE_CONTAINER_FORM_PARAMS,
            RESTEASY_WIDER_REQUEST_MATCHING
    };
}
