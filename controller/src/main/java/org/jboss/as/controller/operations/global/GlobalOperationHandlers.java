/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.operations.global;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Global {@code OperationHandler}s.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class GlobalOperationHandlers {

    /*
   ************** Shared parameter definitions ***************
    */

    static final SimpleAttributeDefinition RECURSIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final SimpleAttributeDefinition RECURSIVE_DEPTH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE_DEPTH, ModelType.INT)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(0))
            .build();

    static final SimpleAttributeDefinition PROXIES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PROXIES, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final SimpleAttributeDefinition INCLUDE_RUNTIME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_RUNTIME, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final SimpleAttributeDefinition INCLUDE_DEFAULTS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_DEFAULTS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(true))
            .build();

    static final SimpleAttributeDefinition INCLUDE_ALIASES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_ALIASES, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING)
            .setValidator(new StringLengthValidator(1))
            .setAllowNull(false)
            .build();

    static final SimpleAttributeDefinition LOCALE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.LOCALE, ModelType.STRING)
            .setAllowNull(true)
            .build();

    static final SimpleAttributeDefinition CHILD_TYPE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CHILD_TYPE, ModelType.STRING)
            .setValidator(new StringLengthValidator(1))
            .setAllowNull(false)
            .build();

    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALUE, ModelType.STRING)
            .setValidator(new StringLengthValidator(1))
            .setAllowNull(true)
            .build();

    /*
    ************** Operation definitions ***************
     */

    @Deprecated
    public static final OperationDefinition READ_RESOURCE_DEFINITION = org.jboss.as.controller.operations.global.ReadResourceHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_ATTRIBUTE_DEFINITION = org.jboss.as.controller.operations.global.ReadAttributeHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_RESOURCE_DESCRIPTION_DEFINITION = ReadResourceDescriptionHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_CHILDREN_NAMES_DEFINITION = ReadChildrenNamesHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_CHILDREN_TYPES_DEFINITION = ReadChildrenTypesHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_CHILDREN_RESOURCES_DEFINITION = ReadChildrenResourcesHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_OPERATION_NAMES_DEFINITION = ReadOperationNamesHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition READ_OPERATION_DESCRIPTION_DEFINITION = ReadOperationDescriptionHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition UNDEFINE_ATTRIBUTE_DEFINITION = org.jboss.as.controller.operations.global.UndefineAttributeHandler.DEFINITION;

    @Deprecated
    public static final OperationDefinition WRITE_ATTRIBUTE_DEFINITION = org.jboss.as.controller.operations.global.WriteAttributeHandler.DEFINITION;

    @Deprecated
    public static final OperationStepHandler READ_RESOURCE = org.jboss.as.controller.operations.global.ReadResourceHandler.INSTANCE;
    @Deprecated
    public static final OperationStepHandler READ_ATTRIBUTE = org.jboss.as.controller.operations.global.ReadAttributeHandler.INSTANCE;
    @Deprecated
    public static final OperationStepHandler READ_CHILDREN_NAMES = ReadChildrenNamesHandler.INSTANCE;
    @Deprecated
    public static final OperationStepHandler READ_CHILDREN_RESOURCES = ReadChildrenResourcesHandler.INSTANCE;
    @Deprecated
    public static final OperationStepHandler UNDEFINE_ATTRIBUTE = org.jboss.as.controller.operations.global.UndefineAttributeHandler.INSTANCE;
    @Deprecated
    public static final OperationStepHandler WRITE_ATTRIBUTE = org.jboss.as.controller.operations.global.WriteAttributeHandler.INSTANCE;


    public static void registerGlobalOperations(ManagementResourceRegistration root, ProcessType processType) {
        root.registerOperationHandler(org.jboss.as.controller.operations.global.ReadResourceHandler.DEFINITION,
                                      org.jboss.as.controller.operations.global.ReadResourceHandler.INSTANCE, true);
        root.registerOperationHandler(org.jboss.as.controller.operations.global.ReadAttributeHandler.DEFINITION,
                                      org.jboss.as.controller.operations.global.ReadAttributeHandler.INSTANCE, true);
        root.registerOperationHandler(ReadResourceDescriptionHandler.DEFINITION, ReadResourceDescriptionHandler.INSTANCE, true);
        root.registerOperationHandler(ReadChildrenNamesHandler.DEFINITION, ReadChildrenNamesHandler.INSTANCE, true);
        root.registerOperationHandler(ReadChildrenTypesHandler.DEFINITION, ReadChildrenTypesHandler.INSTANCE, true);
        root.registerOperationHandler(ReadChildrenResourcesHandler.DEFINITION, ReadChildrenResourcesHandler.INSTANCE, true);
        root.registerOperationHandler(ReadOperationNamesHandler.DEFINITION, ReadOperationNamesHandler.INSTANCE, true);
        root.registerOperationHandler(ReadOperationDescriptionHandler.DEFINITION, ReadOperationDescriptionHandler.INSTANCE, true);
        if (processType != ProcessType.DOMAIN_SERVER) {
            root.registerOperationHandler(org.jboss.as.controller.operations.global.WriteAttributeHandler.DEFINITION,
                                          org.jboss.as.controller.operations.global.WriteAttributeHandler.INSTANCE, true);
            root.registerOperationHandler(org.jboss.as.controller.operations.global.UndefineAttributeHandler.DEFINITION,
                                          org.jboss.as.controller.operations.global.UndefineAttributeHandler.INSTANCE, true);
        }
    }

    private GlobalOperationHandlers() {
        //
    }


    /**
     * {@link org.jboss.as.controller.OperationStepHandler} querying the child types of a given node.
     */
    @Deprecated
    public static final OperationStepHandler READ_CHILDREN_TYPES = ReadChildrenTypesHandler.INSTANCE;

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} returning the names of the defined operations at a given model address.
     */
    @Deprecated
    public static final OperationStepHandler READ_OPERATION_NAMES = ReadOperationNamesHandler.INSTANCE;

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} returning the type description of a single operation description.
     */
    @Deprecated
    public static final OperationStepHandler READ_OPERATION_DESCRIPTION = ReadOperationDescriptionHandler.INSTANCE;

    /**
     * {@link org.jboss.as.controller.OperationStepHandler} querying the complete type description of a given model node.
     */
    @Deprecated
    public static final OperationStepHandler READ_RESOURCE_DESCRIPTION = ReadResourceDescriptionHandler.INSTANCE;


    @Deprecated
    public static class ReadResourceHandler extends org.jboss.as.controller.operations.global.ReadResourceHandler {
    }

    @Deprecated
    public static class WriteAttributeHandler extends org.jboss.as.controller.operations.global.WriteAttributeHandler {
    }

    @Deprecated
    public static class ReadAttributeHandler extends org.jboss.as.controller.operations.global.ReadAttributeHandler {
    }

    @Deprecated
    public static class UndefineAttributeHandler extends org.jboss.as.controller.operations.global.UndefineAttributeHandler {
    }

    @Deprecated
    public static class ReadChildrenNamesOperationHandler extends org.jboss.as.controller.operations.global.ReadChildrenNamesHandler {
    }

    @Deprecated
    public static class ReadChildrenResourcesOperationHandler extends org.jboss.as.controller.operations.global.ReadChildrenResourcesHandler {
    }

    public abstract static class AbstractMultiTargetHandler implements OperationStepHandler {

        public static final ModelNode FAKE_OPERATION;

        static {
            final ModelNode resolve = new ModelNode();
            resolve.get(OP).set("resolve");
            resolve.get(OP_ADDR).setEmptyList();
            resolve.protect();
            FAKE_OPERATION = resolve;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            // In case if it's a multiTarget operation, resolve the address first
            // This only works for model resources, which can be resolved into a concrete addresses
            if (address.isMultiTarget()) {
                // The final result should be a list of executed operations
                final ModelNode result = context.getResult().setEmptyList();
                // Trick the context to give us the model-root
                context.addStep(new ModelNode(), FAKE_OPERATION.clone(), new ModelAddressResolver(operation, result, new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        doExecute(context, operation);
                    }
                }), OperationContext.Stage.IMMEDIATE);
                context.stepCompleted();
            } else {
                doExecute(context, operation);
            }
        }

        /**
         * Execute the actual operation if it is not addressed to multiple targets.
         *
         * @param context   the operation context
         * @param operation the original operation
         * @throws OperationFailedException
         */
        abstract void doExecute(OperationContext context, ModelNode operation) throws OperationFailedException;
    }

    public static final class ModelAddressResolver implements OperationStepHandler {

        private final ModelNode operation;
        private final ModelNode result;
        private final OperationStepHandler handler; // handler bypassing further wildcard resolution

        public ModelAddressResolver(final ModelNode operation, final ModelNode result, final OperationStepHandler delegate) {
            this.operation = operation;
            this.result = result;
            this.handler = delegate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            execute(address, PathAddress.EMPTY_ADDRESS, context);
            context.stepCompleted();
        }

        void execute(final PathAddress address, final PathAddress base, final OperationContext context) {
            final Resource resource = context.readResource(base, false);
            final PathAddress current = address.subAddress(base.size());
            final Iterator<PathElement> iterator = current.iterator();
            if (iterator.hasNext()) {
                final PathElement element = iterator.next();
                if (element.isMultiTarget()) {
                    final String childType = element.getKey().equals("*") ? null : element.getKey();
                    final ImmutableManagementResourceRegistration registration = context.getResourceRegistration().getSubModel(base);
                    if (registration.isRemote() || registration.isRuntimeOnly()) {
                        // At least for proxies it should use the proxy operation handler
                        throw new IllegalStateException();
                    }
                    final Map<String, Set<String>> resolved = getChildAddresses(context, address, registration, resource, childType);
                    for (Map.Entry<String, Set<String>> entry : resolved.entrySet()) {
                        final String key = entry.getKey();
                        final Set<String> children = entry.getValue();
                        if (children.isEmpty()) {
                            continue;
                        }
                        if (element.isWildcard()) {
                            for (final String child : children) {
                                // Double check if the child actually exists
                                if (resource.hasChild(PathElement.pathElement(key, child))) {
                                    execute(address, base.append(PathElement.pathElement(key, child)), context);
                                }
                            }
                        } else {
                            for (final String segment : element.getSegments()) {
                                if (children.contains(segment)) {
                                    // Double check if the child actually exists
                                    if (resource.hasChild(PathElement.pathElement(key, segment))) {
                                        execute(address, base.append(PathElement.pathElement(key, segment)), context);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Double check if the child actually exists
                    if (resource.hasChild(element)) {
                        execute(address, base.append(element), context);
                    }
                }
            } else {
                //final String operationName = operation.require(OP).asString();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(base.toModelNode());

                final ModelNode result = this.result.add();
                result.get(OP_ADDR).set(base.toModelNode());
                context.addStep(result, newOp, handler, OperationContext.Stage.IMMEDIATE);
            }
        }

    }

    static class RegistrationAddressResolver implements OperationStepHandler {

        private final ModelNode operation;
        private final ModelNode result;
        private final OperationStepHandler handler; // handler bypassing further wildcard resolution

        RegistrationAddressResolver(final ModelNode operation, final ModelNode result, final OperationStepHandler delegate) {
            this.operation = operation;
            this.result = result;
            this.handler = delegate;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode ignored) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            execute(address, PathAddress.EMPTY_ADDRESS, context);
            context.stepCompleted();
        }

        void execute(final PathAddress address, PathAddress base, final OperationContext context) {
            final PathAddress current = address.subAddress(base.size());
            final Iterator<PathElement> iterator = current.iterator();
            if (iterator.hasNext()) {
                final PathElement element = iterator.next();
                if (element.isMultiTarget()) {
                    final Set<PathElement> children = context.getResourceRegistration().getChildAddresses(base);
                    if (children == null || children.isEmpty()) {
                        return;
                    }
                    final String childType = element.getKey().equals("*") ? null : element.getKey();
                    for (final PathElement path : children) {
                        if (childType != null && !childType.equals(path.getKey())) {
                            continue;
                        }
                        execute(address, base.append(path), context);
                    }
                } else {
                    execute(address, base.append(element), context);
                }
            } else {
                //final String operationName = operation.require(OP).asString();
                final ModelNode newOp = operation.clone();
                newOp.get(OP_ADDR).set(base.toModelNode());

                final ModelNode result = this.result.add();
                result.get(OP_ADDR).set(base.toModelNode());
                context.addStep(result, newOp, handler, OperationContext.Stage.IMMEDIATE);
            }
        }
    }

    /**
     * Gets the addresses of the child resources under the given resource.
     *
     * @param context        the operation context
     * @param registry       registry entry representing the resource
     * @param resource       the current resource
     * @param validChildType a single child type to which the results should be limited. If {@code null} the result
     *                       should include all child types
     * @return map where the keys are the child types and the values are a set of child names associated with a type
     */
    static Map<String, Set<String>> getChildAddresses(final OperationContext context, final PathAddress addr, final ImmutableManagementResourceRegistration registry, Resource resource, final String validChildType) {

        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        Set<PathElement> elements = registry.getChildAddresses(PathAddress.EMPTY_ADDRESS);
        for (PathElement element : elements) {
            String childType = element.getKey();
            if (validChildType != null && !validChildType.equals(childType)) {
                continue;
            }
            final ImmutableManagementResourceRegistration childRegistration = registry.getSubModel(PathAddress.pathAddress(element));
            final AliasEntry aliasEntry = childRegistration.getAliasEntry();

            Set<String> set = result.get(childType);
            if (set == null) {
                set = new LinkedHashSet<String>();
                result.put(childType, set);
            }

            if (aliasEntry == null) {
                if (resource.hasChildren(childType)) {
                    set.addAll(resource.getChildrenNames(childType));
                }
            } else {
                //PathAddress target = aliasEntry.getTargetAddress();
                PathAddress target = aliasEntry.convertToTargetAddress(addr.append(element));
                PathAddress targetParent = target.subAddress(0, target.size() - 1);
                Resource parentResource = context.readResourceFromRoot(targetParent);
                if (parentResource.hasChildren(target.getLastElement().getKey())) {
                    set.add(element.getValue());
                }
            }
            if (!element.isWildcard()) {
                ImmutableManagementResourceRegistration childReg = registry.getSubModel(PathAddress.pathAddress(element));
                if (childReg != null && childReg.isRuntimeOnly()) {
                    set.add(element.getValue());
                }
            }
        }

        return result;
    }

    static Locale getLocale(OperationContext context, final ModelNode operation) throws OperationFailedException {
        if (!operation.hasDefined(LOCALE.getName())) {
            return null;
        }
        String unparsed = normalizeLocale(operation.get(LOCALE.getName()).asString());
        int len = unparsed.length();
        if (len != 2 && len != 5 && len < 7) {
            reportInvalidLocaleFormat(context, unparsed);
            return null;
        }

        char char0 = unparsed.charAt(0);
        char char1 = unparsed.charAt(1);
        if (char0 < 'a' || char0 > 'z' || char1 < 'a' || char1 > 'z') {
            reportInvalidLocaleFormat(context, unparsed);
            return null;
        }
        if (len == 2) {
            return new Locale(unparsed, "");
        }

        if (!isLocaleSeparator(unparsed.charAt(2))) {
            reportInvalidLocaleFormat(context, unparsed);
            return null;
        }
        char char3 = unparsed.charAt(3);
        if (isLocaleSeparator(char3)) {
            // no country
            return new Locale(unparsed.substring(0, 2), "", unparsed.substring(4));
        }

        char char4 = unparsed.charAt(4);
        if (char3 < 'A' || char3 > 'Z' || char4 < 'A' || char4 > 'Z') {
            reportInvalidLocaleFormat(context, unparsed);
            return null;
        }
        if (len == 5) {
            return new Locale(unparsed.substring(0, 2), unparsed.substring(3));
        }

        if (!isLocaleSeparator(unparsed.charAt(5))) {
            reportInvalidLocaleFormat(context, unparsed);
            return null;
        }
        return new Locale(unparsed.substring(0, 2), unparsed.substring(3, 5), unparsed.substring(6));
    }

    private static String normalizeLocale(String toNormalize) {
        return ("zh_Hans".equalsIgnoreCase(toNormalize) || "zh-Hans".equalsIgnoreCase(toNormalize)) ? "zh_CN" : toNormalize;
    }

    private static boolean isLocaleSeparator(char ch) {
        return ch == '-' || ch == '_';
    }

    private static void reportInvalidLocaleFormat(OperationContext context, String format) {
        String msg = MESSAGES.invalidLocaleString(format);
        ControllerLogger.MGMT_OP_LOGGER.debug(msg);
        // TODO report the problem to client via out-of-band message.
        // Enable this in 7.2 or later when there is time to test
        //context.report(MessageSeverity.WARN, msg);
    }


}
