/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:47
 */
class JSPDefinition extends SimplePersistentResourceDefinition {
    static final JSPDefinition INSTANCE = new JSPDefinition();

    protected static final SimpleAttributeDefinition DEVELOPMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DEVELOPMENT, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition DISABLED =
            new SimpleAttributeDefinitionBuilder(Constants.DISABLED, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition KEEP_GENERATED =
            new SimpleAttributeDefinitionBuilder(Constants.KEEP_GENERATED, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition TRIM_SPACES =
            new SimpleAttributeDefinitionBuilder(Constants.TRIM_SPACES, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition TAG_POOLING =
            new SimpleAttributeDefinitionBuilder(Constants.TAG_POOLING, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MAPPED_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.MAPPED_FILE, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition CHECK_INTERVAL =
            new SimpleAttributeDefinitionBuilder(Constants.CHECK_INTERVAL, ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(0))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MODIFICATION_TEST_INTERVAL =
            new SimpleAttributeDefinitionBuilder(Constants.MODIFICATION_TEST_INTERVAL, ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(4))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition RECOMPILE_ON_FAIL =
            new SimpleAttributeDefinitionBuilder(Constants.RECOMPILE_ON_FAIL, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SMAP =
            new SimpleAttributeDefinitionBuilder(Constants.SMAP, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition DUMP_SMAP =
            new SimpleAttributeDefinitionBuilder(Constants.DUMP_SMAP, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition GENERATE_STRINGS_AS_CHAR_ARRAYS =
            new SimpleAttributeDefinitionBuilder(Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SCRATCH_DIR =
            new SimpleAttributeDefinitionBuilder(Constants.SCRATCH_DIR, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SOURCE_VM =
            new SimpleAttributeDefinitionBuilder(Constants.SOURCE_VM, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("1.6"))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition TARGET_VM =
            new SimpleAttributeDefinitionBuilder(Constants.TARGET_VM, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("1.6"))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition JAVA_ENCODING =
            new SimpleAttributeDefinitionBuilder(Constants.JAVA_ENCODING, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("UTF8"))
                    .setAllowExpression(true)
                    .build();


    protected static final SimpleAttributeDefinition X_POWERED_BY =
            new SimpleAttributeDefinitionBuilder(Constants.X_POWERED_BY, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition DISPLAY_SOURCE_FRAGMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DISPLAY_SOURCE_FRAGMENT, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(new ModelNode(true))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition[] ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            DEVELOPMENT,
            DISABLED,
            KEEP_GENERATED,
            TRIM_SPACES,
            TAG_POOLING,
            MAPPED_FILE,
            CHECK_INTERVAL,
            MODIFICATION_TEST_INTERVAL,
            RECOMPILE_ON_FAIL,
            SMAP,
            DUMP_SMAP,
            GENERATE_STRINGS_AS_CHAR_ARRAYS,
            ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE,
            SCRATCH_DIR,
            SOURCE_VM,
            TARGET_VM,
            JAVA_ENCODING,
            X_POWERED_BY,
            DISPLAY_SOURCE_FRAGMENT
    };
    static final Map<String, AttributeDefinition> ATTRIBUTES_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            ATTRIBUTES_MAP.put(attr.getName(), attr);
        }
    }

    private JSPDefinition() {
        super(UndertowExtension.PATH_JSP,
                UndertowExtension.getResolver(UndertowExtension.PATH_JSP.getKeyValuePair()),
                new JSPAdd(),
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    @Override
    public String getXmlElementName() {
        return Constants.JSP_CONFIG;
    }

    private static class JSPAdd extends AbstractBoottimeAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
            try {
                Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, this.getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                UndertowLogger.ROOT_LOGGER.couldNotInitJsp(e);
            }
        }
    }
}
