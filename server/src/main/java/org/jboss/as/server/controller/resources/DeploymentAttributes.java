/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.server.ServerMessages.MESSAGES;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.deployment.AbstractDeploymentUnitService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DeploymentAttributes {

    public static ResourceDescriptionResolver DEPLOYMENT_RESOLVER = ServerDescriptions.getResourceDescriptionResolver(DEPLOYMENT, false);

    //Top level attributes
    public static final SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING, false)
        .setValidator(new StringLengthValidator(1, false))
        .build();
    public static final AttributeDefinition TO_REPLACE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.TO_REPLACE, NAME).build();
    public static final SimpleAttributeDefinition RUNTIME_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.RUNTIME_NAME, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, true))
        .build();
    public static final SimpleAttributeDefinition ENABLED = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ENABLED, ModelType.BOOLEAN, true)
         .setDefaultValue(new ModelNode(false))
        .build();
    public static final AttributeDefinition PERSISTENT = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PERSISTENT, ModelType.BOOLEAN, false)
        .build();
    public static final AttributeDefinition STATUS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.STATUS, ModelType.STRING, true)
        .setValidator(new EnumValidator<AbstractDeploymentUnitService.DeploymentStatus>(AbstractDeploymentUnitService.DeploymentStatus.class, true))
        .build();

    //Managed content value attributes
    public static final SimpleAttributeDefinition CONTENT_INPUT_STREAM_INDEX =
            createContentValueTypeAttribute(ModelDescriptionConstants.INPUT_STREAM_INDEX, ModelType.INT, new StringLengthValidator(1, true), false);
    public static final SimpleAttributeDefinition CONTENT_HASH =
            createContentValueTypeAttribute(ModelDescriptionConstants.HASH, ModelType.BYTES, new HashValidator(true), false);
    public static final SimpleAttributeDefinition CONTENT_BYTES =
            createContentValueTypeAttribute(ModelDescriptionConstants.BYTES, ModelType.BYTES, new ModelTypeValidator(ModelType.BYTES, true), false);
    public static final SimpleAttributeDefinition CONTENT_URL =
            createContentValueTypeAttribute(ModelDescriptionConstants.URL, ModelType.STRING, new StringLengthValidator(1, true), false);

    //Unmanaged content value attributes
    public static final AttributeDefinition CONTENT_PATH =
            createContentValueTypeAttribute(ModelDescriptionConstants.PATH, ModelType.STRING, new StringLengthValidator(1, true), false);
    public static final AttributeDefinition CONTENT_RELATIVE_TO =
            createContentValueTypeAttribute(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, new StringLengthValidator(1, true), false);
    public static final AttributeDefinition CONTENT_ARCHIVE =
            createContentValueTypeAttribute(ModelDescriptionConstants.ARCHIVE, ModelType.STRING, new StringLengthValidator(1, true), false);

    /** The content complex attribute */
    public static final ObjectListAttributeDefinition CONTENT_ALL =
                ObjectListAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                    ObjectTypeAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                            CONTENT_INPUT_STREAM_INDEX,
                            CONTENT_HASH,
                            CONTENT_BYTES,
                            CONTENT_URL,
                            CONTENT_PATH,
                            CONTENT_RELATIVE_TO,
                            CONTENT_ARCHIVE)
                            .setValidator(new ContentTypeValidator())
                            .build())
                    .setMinSize(1)
                    .setMaxSize(1)
                    .build();
    public static final ObjectListAttributeDefinition CONTENT_ALL_NILLABLE =
            ObjectListAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                ObjectTypeAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                        CONTENT_INPUT_STREAM_INDEX,
                        CONTENT_HASH,
                        CONTENT_BYTES,
                        CONTENT_URL,
                        CONTENT_PATH,
                        CONTENT_RELATIVE_TO,
                        CONTENT_ARCHIVE)
                        .setValidator(new ContentTypeValidator())
                        .build())
                .setMinSize(1)
                .setMaxSize(1)
                .setAllowNull(true)
                .build();
    public static final ObjectListAttributeDefinition CONTENT_RESOURCE =
                ObjectListAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                    ObjectTypeAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                            CONTENT_HASH,
                            CONTENT_PATH,
                            CONTENT_RELATIVE_TO,
                            CONTENT_ARCHIVE)
                            .setValidator(new ContentTypeValidator())
                            .build())
                    .setMinSize(1)
                    .setMaxSize(1)
                    .build();

    /** Attributes for server deployment resource */
    public static final AttributeDefinition[] SERVER_RESOURCE_ATTRIBUTES = new AttributeDefinition[] {NAME, RUNTIME_NAME, CONTENT_RESOURCE, ENABLED, PERSISTENT, STATUS};

    /** Attributes for server deployment add */
    public static final AttributeDefinition[] SERVER_ADD_ATTRIBUTES = new AttributeDefinition[] { RUNTIME_NAME, CONTENT_ALL, ENABLED, PERSISTENT, STATUS};

    /** Attributes for server group deployment add */
    public static final AttributeDefinition[] SERVER_GROUP_RESOURCE_ATTRIBUTES = new AttributeDefinition[] {NAME, RUNTIME_NAME, ENABLED};

    /** Attributes for server group deployment add */
    public static final AttributeDefinition[] SERVER_GROUP_ADD_ATTRIBUTES = new AttributeDefinition[] {RUNTIME_NAME, ENABLED};

    /** Attributes for domain deployment resource */
    public static final AttributeDefinition[] DOMAIN_RESOURCE_ATTRIBUTES = new AttributeDefinition[] {NAME, RUNTIME_NAME, CONTENT_RESOURCE};

    /** Attributes for domain deployment add */
    public static final AttributeDefinition[] DOMAIN_ADD_ATTRIBUTES = new AttributeDefinition[] {RUNTIME_NAME, CONTENT_ALL};

    /** Attributes indicating managed deployments in the content attribute */
    public static final Map<String, AttributeDefinition> MANAGED_CONTENT_ATTRIBUTES = createAttributeMap(CONTENT_INPUT_STREAM_INDEX, CONTENT_HASH, CONTENT_BYTES, CONTENT_URL);

    /** Attributes indicating unmanaged deployments in the content attribute */
    public static final Map<String, AttributeDefinition> UNMANAGED_CONTENT_ATTRIBUTES = createAttributeMap(CONTENT_PATH, CONTENT_RELATIVE_TO, CONTENT_ARCHIVE);

    /** All attributes of the content attribute */
    public static final Map<String, AttributeDefinition> ALL_CONTENT_ATTRIBUTES = createAttributeMap(MANAGED_CONTENT_ATTRIBUTES, UNMANAGED_CONTENT_ATTRIBUTES);

    public static OperationDefinition DEPLOY_DEFINITION = new SimpleOperationDefinition(ModelDescriptionConstants.DEPLOY, DEPLOYMENT_RESOLVER);
    public static OperationDefinition UNDEPLOY_DEFINITION = new SimpleOperationDefinition(ModelDescriptionConstants.UNDEPLOY, DEPLOYMENT_RESOLVER);
    public static OperationDefinition REDEPLOY_DEFINITION = new SimpleOperationDefinition(ModelDescriptionConstants.REDEPLOY, DEPLOYMENT_RESOLVER);

    /** Server add deployment definition */
    public static OperationDefinition SERVER_DEPLOYMENT_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, DEPLOYMENT_RESOLVER)
            .setParameters(new AttributeDefinition[] { RUNTIME_NAME, CONTENT_ALL, ENABLED, /*PERSISTENT,*/}) // 'hide' the persistent attribute from users
            .build();

    /** Server group add deployment definition */
    public static OperationDefinition SERVER_GROUP_DEPLOYMENT_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, DEPLOYMENT_RESOLVER)
            .setParameters(SERVER_GROUP_ADD_ATTRIBUTES)
            .build();

    public static OperationDefinition DOMAIN_DEPLOYMENT_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.ADD, DEPLOYMENT_RESOLVER)
            .setParameters(DOMAIN_ADD_ATTRIBUTES)
            .build();

    /** Return type for the upload-deployment-xxx operaions */
    private static SimpleAttributeDefinition UPLOAD_HASH_REPLY = SimpleAttributeDefinitionBuilder.create(CONTENT_HASH)
            .setAllowNull(false)
            .build();

    //Upload deployment bytes definitions
    public static AttributeDefinition BYTES_NOT_NULL = SimpleAttributeDefinitionBuilder.create(DeploymentAttributes.CONTENT_BYTES)
            .setAllowNull(false)
            .build();
    public static OperationDefinition UPLOAD_BYTES_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES, DEPLOYMENT_RESOLVER)
            .setParameters(BYTES_NOT_NULL)
            .setReplyParameters(UPLOAD_HASH_REPLY)
            .build();
    public static OperationDefinition DOMAIN_UPLOAD_BYTES_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES, DEPLOYMENT_RESOLVER)
            .setParameters(BYTES_NOT_NULL)
            .setReplyParameters(UPLOAD_HASH_REPLY)
            .withFlag(Flag.MASTER_HOST_CONTROLLER_ONLY)
            .build();

    //Upload deployment url definitions
    public static AttributeDefinition URL_NOT_NULL = SimpleAttributeDefinitionBuilder.create(DeploymentAttributes.CONTENT_URL)
            .setAllowNull(false)
            .build();
    public static OperationDefinition UPLOAD_URL_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_URL, DEPLOYMENT_RESOLVER)
            .setParameters(URL_NOT_NULL)
            .setReplyParameters(UPLOAD_HASH_REPLY)
            .build();
    public static OperationDefinition DOMAIN_UPLOAD_URL_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_URL, DEPLOYMENT_RESOLVER)
            .setParameters(URL_NOT_NULL)
            .setReplyParameters(UPLOAD_HASH_REPLY)
            .withFlag(Flag.MASTER_HOST_CONTROLLER_ONLY)
            .build();

    //Upload deployment stream definition
    public static AttributeDefinition INPUT_STREAM_INDEX_NOT_NULL = SimpleAttributeDefinitionBuilder.create(DeploymentAttributes.CONTENT_INPUT_STREAM_INDEX)
            .setAllowNull(false)
            .build();
    public static Map<String, AttributeDefinition> UPLOAD_INPUT_STREAM_INDEX_ATTRIBUTES = Collections.singletonMap(INPUT_STREAM_INDEX_NOT_NULL.getName(), INPUT_STREAM_INDEX_NOT_NULL);
    public static OperationDefinition UPLOAD_STREAM_ATTACHMENT_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_STREAM, DEPLOYMENT_RESOLVER)
            .setParameters(INPUT_STREAM_INDEX_NOT_NULL)
            .setReplyParameters(UPLOAD_HASH_REPLY)
            .build();
    public static OperationDefinition DOMAIN_UPLOAD_STREAM_ATTACHMENT_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_STREAM, DEPLOYMENT_RESOLVER)
            .setParameters(INPUT_STREAM_INDEX_NOT_NULL)
            .setReplyParameters(UPLOAD_HASH_REPLY)
            .withFlag(Flag.MASTER_HOST_CONTROLLER_ONLY)
            .build();

    //Replace deployment definition
    public static Map<String, AttributeDefinition> REPLACE_DEPLOYMENT_ATTRIBUTES = createAttributeMap(NAME, TO_REPLACE, CONTENT_ALL_NILLABLE, RUNTIME_NAME);
    public static OperationDefinition REPLACE_DEPLOYMENT_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REPLACE_DEPLOYMENT, DEPLOYMENT_RESOLVER)
            .setParameters(REPLACE_DEPLOYMENT_ATTRIBUTES.values().toArray(new AttributeDefinition[REPLACE_DEPLOYMENT_ATTRIBUTES.size()]))
            .build();

    public static Map<String, AttributeDefinition> SERVER_GROUP_REPLACE_DEPLOYMENT_ATTRIBUTES = createAttributeMap(NAME, TO_REPLACE, RUNTIME_NAME);
    public static OperationDefinition SERVER_GROUP_REPLACE_DEPLOYMENT_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.REPLACE_DEPLOYMENT, DEPLOYMENT_RESOLVER)
            .setParameters(SERVER_GROUP_REPLACE_DEPLOYMENT_ATTRIBUTES.values().toArray(new AttributeDefinition[SERVER_GROUP_REPLACE_DEPLOYMENT_ATTRIBUTES.size()]))
            .build();

    //Full replace deployment definition
    public static Map<String, AttributeDefinition> FULL_REPLACE_DEPLOYMENT_ATTRIBUTES = createAttributeMap(NAME, RUNTIME_NAME, CONTENT_ALL_NILLABLE);
    public static OperationDefinition FULL_REPLACE_DEPLOYMENT_DEFINITION = new SimpleOperationDefinitionBuilder(ModelDescriptionConstants.FULL_REPLACE_DEPLOYMENT, DEPLOYMENT_RESOLVER)
            .setParameters(FULL_REPLACE_DEPLOYMENT_ATTRIBUTES.values().toArray(new AttributeDefinition[FULL_REPLACE_DEPLOYMENT_ATTRIBUTES.size()]))
             .build();

    private static SimpleAttributeDefinition createContentValueTypeAttribute(String name, ModelType type, ParameterValidator validator, boolean allowExpression) {
        SimpleAttributeDefinitionBuilder builder = SimpleAttributeDefinitionBuilder.create(name, type, true);
        if (validator != null) {
            builder.setValidator(validator);
        }
        builder.setAllowExpression(allowExpression);
        return builder.build();
    }

    private static class HashValidator extends ModelTypeValidator implements MinMaxValidator {
        public HashValidator(boolean nillable) {
            super(ModelType.BYTES, nillable);
        }

        @Override
        public Long getMin() {
            return 20L;
        }

        @Override
        public Long getMax() {
            return 20L;
        }
    }

    private static Map<String, AttributeDefinition> createAttributeMap(AttributeDefinition...defs) {
        Map<String, AttributeDefinition> map = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition def : defs) {
            map.put(def.getName(), def);
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, AttributeDefinition> createAttributeMap(Map<String, AttributeDefinition>...maps) {
        Map<String, AttributeDefinition> map = new HashMap<String, AttributeDefinition>();
        for (Map<String, AttributeDefinition> other : maps) {
            map.putAll(other);
        }
        return Collections.unmodifiableMap(map);
    }

    private static class ContentTypeValidator extends ParametersValidator {

        @Override
        public void validateParameter(String parameterName, ModelNode contentItemNode) throws OperationFailedException {

            Set<String> managedNames = new HashSet<String>();
            Set<String> unmanagedNames = new HashSet<String>();
            for (String name : contentItemNode.keys()) {
                if (contentItemNode.hasDefined(name)) {
                    if (MANAGED_CONTENT_ATTRIBUTES.containsKey(name)) {
                        managedNames.add(name);
                    } else if (UNMANAGED_CONTENT_ATTRIBUTES.containsKey(name)) {
                        unmanagedNames.add(name);
                    } else {
                        throw MESSAGES.unknownContentItemKey(name);
                    }
                }
            }
            if (managedNames.size() > 1) {
                //TODO i18n
                throw MESSAGES.cannotHaveMoreThanOneManagedContentItem(MANAGED_CONTENT_ATTRIBUTES.keySet());
            }
            if (unmanagedNames.size() > 0 && managedNames.size() > 0) {
                throw MESSAGES.cannotMixUnmanagedAndManagedContentItems(managedNames, unmanagedNames);
            }
            if (unmanagedNames.size() > 0) {
                if (!unmanagedNames.contains(CONTENT_ARCHIVE.getName())) {
                    throw MESSAGES.nullParameter(CONTENT_ARCHIVE.getName());
                }
                if (!unmanagedNames.contains(CONTENT_PATH.getName())) {
                    throw MESSAGES.nullParameter(CONTENT_PATH.getName());
                }
            }

            for (String key : contentItemNode.keys()){

                AttributeDefinition def = MANAGED_CONTENT_ATTRIBUTES.get(key);
                if (def == null) {
                    def = UNMANAGED_CONTENT_ATTRIBUTES.get(key);
                }
                if (def != null) {
                    def.validateOperation(contentItemNode);
                }
            }
        }
    }

}
