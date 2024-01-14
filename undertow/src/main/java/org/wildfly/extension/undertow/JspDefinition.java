/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.List;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:47
 */
class JspDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.JSP);
    protected static final SimpleAttributeDefinition DEVELOPMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DEVELOPMENT, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition DISABLED =
            new SimpleAttributeDefinitionBuilder(Constants.DISABLED, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition KEEP_GENERATED =
            new SimpleAttributeDefinitionBuilder(Constants.KEEP_GENERATED, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.TRUE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition TRIM_SPACES =
            new SimpleAttributeDefinitionBuilder(Constants.TRIM_SPACES, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition TAG_POOLING =
            new SimpleAttributeDefinitionBuilder(Constants.TAG_POOLING, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.TRUE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition MAPPED_FILE =
            new SimpleAttributeDefinitionBuilder(Constants.MAPPED_FILE, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.TRUE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition CHECK_INTERVAL =
            new SimpleAttributeDefinitionBuilder(Constants.CHECK_INTERVAL, ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.ZERO)
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
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition SMAP =
            new SimpleAttributeDefinitionBuilder(Constants.SMAP, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.TRUE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition DUMP_SMAP =
            new SimpleAttributeDefinitionBuilder(Constants.DUMP_SMAP, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition GENERATE_STRINGS_AS_CHAR_ARRAYS =
            new SimpleAttributeDefinitionBuilder(Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(ModelNode.FALSE)
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
                    .setDefaultValue(new ModelNode("1.8"))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition TARGET_VM =
            new SimpleAttributeDefinitionBuilder(Constants.TARGET_VM, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .setDefaultValue(new ModelNode("1.8"))
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
                    .setDefaultValue(ModelNode.TRUE)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition DISPLAY_SOURCE_FRAGMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DISPLAY_SOURCE_FRAGMENT, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(ModelNode.TRUE)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition OPTIMIZE_SCRIPTLETS =
            new SimpleAttributeDefinitionBuilder(Constants.OPTIMIZE_SCRIPTLETS, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(ModelNode.FALSE)
                    .setAllowExpression(true)
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            DISABLED,
            DEVELOPMENT,
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
            DISPLAY_SOURCE_FRAGMENT,
            OPTIMIZE_SCRIPTLETS);

    JspDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKeyValuePair()))
                .setAddHandler(new JSPAdd())
                .setRemoveHandler(new JSPRemove())
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static JSPConfig getConfig(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
        if (!model.isDefined()) {
            return null;
        }
        boolean disabled = DISABLED.resolveModelAttribute(context, model).asBoolean();
        boolean development = DEVELOPMENT.resolveModelAttribute(context, model).asBoolean();
        boolean keepGenerated = KEEP_GENERATED.resolveModelAttribute(context, model).asBoolean();
        boolean trimSpaces = TRIM_SPACES.resolveModelAttribute(context, model).asBoolean();
        boolean tagPooling = TAG_POOLING.resolveModelAttribute(context, model).asBoolean();
        boolean mappedFile = MAPPED_FILE.resolveModelAttribute(context, model).asBoolean();
        int checkInterval = CHECK_INTERVAL.resolveModelAttribute(context, model).asInt();
        int modificationTestInterval = MODIFICATION_TEST_INTERVAL.resolveModelAttribute(context, model).asInt();
        boolean recompileOnFile = RECOMPILE_ON_FAIL.resolveModelAttribute(context, model).asBoolean();
        boolean snap = SMAP.resolveModelAttribute(context, model).asBoolean();
        boolean dumpSnap = DUMP_SMAP.resolveModelAttribute(context, model).asBoolean();
        boolean generateStringsAsCharArrays = GENERATE_STRINGS_AS_CHAR_ARRAYS.resolveModelAttribute(context, model).asBoolean();
        boolean errorOnUseBeanInvalidClassAttribute = ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean();
        final ModelNode scratchDirValue = SCRATCH_DIR.resolveModelAttribute(context, model);
        String scratchDir = scratchDirValue.isDefined() ? scratchDirValue.asString() : null;
        String sourceVm = SOURCE_VM.resolveModelAttribute(context, model).asString();
        String targetVm = TARGET_VM.resolveModelAttribute(context, model).asString();
        String javaEncoding = JAVA_ENCODING.resolveModelAttribute(context, model).asString();
        boolean xPoweredBy = X_POWERED_BY.resolveModelAttribute(context, model).asBoolean();
        boolean displaySourceFragment = DISPLAY_SOURCE_FRAGMENT.resolveModelAttribute(context, model).asBoolean();
        boolean optimizeScriptlets = OPTIMIZE_SCRIPTLETS.resolveModelAttribute(context, model).asBoolean();
        return new JSPConfig(development, disabled, keepGenerated, trimSpaces, tagPooling, mappedFile, checkInterval, modificationTestInterval,
                recompileOnFile, snap, dumpSnap, generateStringsAsCharArrays, errorOnUseBeanInvalidClassAttribute, scratchDir,
                sourceVm, targetVm, javaEncoding, xPoweredBy, displaySourceFragment, optimizeScriptlets);
    }

    private static class JSPAdd extends RestartParentResourceAddHandler {
        protected JSPAdd() {
            super(ServletContainerDefinition.PATH_ELEMENT.getKey());
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.installRuntimeServices(context.getCapabilityServiceTarget(), context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }

    private static class JSPRemove extends RestartParentResourceRemoveHandler {

        protected JSPRemove() {
            super(ServletContainerDefinition.PATH_ELEMENT.getKey());
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.installRuntimeServices(context.getCapabilityServiceTarget(), context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }
}
