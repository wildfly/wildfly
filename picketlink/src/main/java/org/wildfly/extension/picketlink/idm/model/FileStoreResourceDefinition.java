/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
        .setDefaultValue(new ModelNode(true))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ASYNC_WRITE = new SimpleAttributeDefinitionBuilder(ModelElement.FILE_STORE_ASYNC_WRITE.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(new ModelNode(false))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition ASYNC_WRITE_THREAD_POOL = new SimpleAttributeDefinitionBuilder(ModelElement.FILE_STORE_ASYNC_THREAD_POOL.getName(), ModelType.INT, true)
        .setDefaultValue(new ModelNode(5))
        .setAllowExpression(true)
        .build();

    public static final FileStoreResourceDefinition INSTANCE = new FileStoreResourceDefinition(WORKING_DIR, RELATIVE_TO, ALWAYS_CREATE_FILE, ASYNC_WRITE, ASYNC_WRITE_THREAD_POOL, SUPPORT_ATTRIBUTE, SUPPORT_CREDENTIAL);

    private FileStoreResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.FILE_STORE, new IDMConfigAddStepHandler(getModelValidators(), attributes), attributes);
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
