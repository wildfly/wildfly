/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import java.io.File;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.NotEmptyResourceValidationStepHandler;
import org.wildfly.extension.picketlink.common.model.validator.RequiredChildValidationStepHandler;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class FileStoreResourceDefinition extends AbstractIdentityStoreResourceDefinition {

    public static final SimpleAttributeDefinition WORKING_DIR = new SimpleAttributeDefinitionBuilder(ModelElement.FILE_STORE_WORKING_DIR.getName(), ModelType.STRING, true)
        .setDefaultValue(new ModelNode().set("picketlink" + File.separatorChar + "idm"))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_RELATIVE_TO.getName(), ModelType.STRING, true)
        .setDefaultValue(new ModelNode().set(ServerEnvironment.SERVER_DATA_DIR))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ALWAYS_CREATE_FILE = new SimpleAttributeDefinitionBuilder(ModelElement.FILE_STORE_ALWAYS_CREATE_FILE.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.TRUE)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ASYNC_WRITE = new SimpleAttributeDefinitionBuilder(ModelElement.FILE_STORE_ASYNC_WRITE.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.FALSE)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ASYNC_WRITE_THREAD_POOL = new SimpleAttributeDefinitionBuilder(ModelElement.FILE_STORE_ASYNC_THREAD_POOL.getName(), ModelType.INT, true)
        .setDefaultValue(new ModelNode(5))
        .setAllowExpression(true)
        .build();

    public static final FileStoreResourceDefinition INSTANCE = new FileStoreResourceDefinition(WORKING_DIR, RELATIVE_TO, ALWAYS_CREATE_FILE, ASYNC_WRITE, ASYNC_WRITE_THREAD_POOL, SUPPORT_ATTRIBUTE, SUPPORT_CREDENTIAL);

    private FileStoreResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.FILE_STORE, getModelValidators(), attributes);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(SupportedTypesResourceDefinition.INSTANCE, resourceRegistration);
        addChildResourceDefinition(CredentialHandlerResourceDefinition.INSTANCE, resourceRegistration);
    }

    private static ModelValidationStepHandler[] getModelValidators() {
        return new ModelValidationStepHandler[] {
            NotEmptyResourceValidationStepHandler.INSTANCE,
            new RequiredChildValidationStepHandler(ModelElement.SUPPORTED_TYPES)
        };
    }
}
