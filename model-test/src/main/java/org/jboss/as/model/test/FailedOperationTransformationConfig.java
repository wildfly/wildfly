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
package org.jboss.as.model.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.DiscardUndefinedAttributesTransformer;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Sets up how to handle failed transformation for use with
 * {@link ModelTestUtils#checkFailedTransformedAddOperation(ModelTestKernelServices, ModelVersion, ModelNode, FailedOperationTransformationConfig)}
 *
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class FailedOperationTransformationConfig {

    public static final FailedOperationTransformationConfig NO_FAILURES = new FailedOperationTransformationConfig();

    private PathAddressConfigRegistry registry = new PathAddressConfigRegistry();

    /**
     * Add a handler for failed operation transformers at a resource address
     *
     *  @param pathAddress the path address
     *  @param config the config
     *  @return this setup
     */
    public FailedOperationTransformationConfig addFailedAttribute(PathAddress pathAddress, PathAddressConfig config) {
        registry.addConfig(pathAddress.iterator(), config);
        return this;
    }

    boolean expectFailed(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null) {
            return false;
        }
        if (cfg.expectFailed(operation)) {
            return true;
        }
        return false;
    }

    boolean expectDiscarded(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null) {
            return false;
        }
        if (cfg.expectDiscarded(operation)) {
            return true;
        }
        return false;
    }

    boolean canCorrectMore(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null) {
            return false;
        }
        if (cfg.canCorrectMore(operation)) {
            return true;
        }
        return false;
    }

    ModelNode correctOperation(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null) {
            throw new IllegalStateException("No path address config found for " + PathAddress.pathAddress(operation.get(OP_ADDR)));
        }
        operation = cfg.correctOperation(operation);
        return operation;
    }

    List<ModelNode> createWriteAttributeOperations(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null || !operation.get(OP).asString().equals(ADD)) {
            return Collections.emptyList();
        }
        return cfg.createWriteAttributeOperations(operation);
    }

    boolean expectFailedWriteAttributeOperation(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null) {
            return false;
        }
        return cfg.expectFailedWriteAttributeOperation(operation);
    }

    ModelNode correctWriteAttributeOperation(ModelNode operation) {
        PathAddressConfig cfg = registry.getConfig(operation);
        if (cfg == null) {
            return operation;
        }
        return cfg.correctWriteAttributeOperation(operation);
    }

    private static class PathAddressConfigRegistry {
        private final Map<PathElement,PathAddressConfigRegistry> children = new HashMap<PathElement, FailedOperationTransformationConfig.PathAddressConfigRegistry>();
        private final Map<String, PathAddressConfigRegistry> wildcardChildren = new HashMap<String, FailedOperationTransformationConfig.PathAddressConfigRegistry>();
        private PathAddressConfig config;

        void addConfig(Iterator<PathElement> address, PathAddressConfig config) {
            if (address.hasNext()) {
                PathAddressConfigRegistry child = getOrCreateConfig(address.next());
                child.addConfig(address, config);
            } else {
                this.config = config;
            }
        }

        PathAddressConfigRegistry getOrCreateConfig(PathElement element) {
            if (element.isWildcard()) {
                PathAddressConfigRegistry reg = wildcardChildren.get(element.getKey());
                if (reg == null) {
                    reg = new PathAddressConfigRegistry();
                    wildcardChildren.put(element.getKey(), reg);
                }
                return reg;
            } else {
                PathAddressConfigRegistry reg = children.get(element);
                if (reg == null) {
                    reg = new PathAddressConfigRegistry();
                    children.put(element, reg);
                }
                return reg;
            }
        }

        PathAddressConfig getConfig(ModelNode operation) {
            return getConfig(PathAddress.pathAddress(operation.get(OP_ADDR)).iterator());
        }

        PathAddressConfig getConfig(Iterator<PathElement> address) {
            if (address.hasNext()) {
                PathElement element = address.next();
                PathAddressConfigRegistry child = children.get(element);
                if (child == null) {
                    child = wildcardChildren.get(element.getKey());
                }
                if (child != null) {
                    return child.getConfig(address);
                }
                return null;
            } else {
                return config;
            }
        }

    }


    /**
     * Configuration of how to deal with rejected operations transformations for a PathAddress.
     * See {@link ModelTestUtils#checkFailedTransformedBootOperations(ModelTestKernelServices, org.jboss.as.controller.ModelVersion, List, FailedOperationTransformationConfig)}
     * for the mechanics of how it is used
     */
    public interface PathAddressConfig {

        /**
         * Whether it is expected that the following operation should fail
         *
         * @return {@code true} if expected to fail
         */
        boolean expectFailed(ModelNode operation);

        /**
         * Whether something can be corrected in the operation to make it pass.
         * It is preferable to correct one attribute at a time.
         *
         * @return {@code true} if expected to fail, {@code false} otherwise
         */
        boolean canCorrectMore(ModelNode operation);

        /**
         * Correct the operation, only called if {@link #canCorrectMore(ModelNode)} returned {@code true}
         * It is preferable to correct one attribute at a time.
         *
         * @return the corrected operation
         */
        ModelNode correctOperation(ModelNode operation);

        /**
         * Creates write attribute operations for the add operations
         */
        List<ModelNode> createWriteAttributeOperations(ModelNode operation);

        /**
         * Whether it is expected that the following write attribute operation should fail
         *
         * @return {@code true} if expected to fail
         */
        boolean expectFailedWriteAttributeOperation(ModelNode operation);

        /**
         * Correct the operation.
         *
         * @return the corrected operation
         */
        ModelNode correctWriteAttributeOperation(ModelNode operation);

        /**
         * Whether the operation is expected to be discarded
         *
         * @return {@code true} if expected to fail
         */
        boolean expectDiscarded(ModelNode operation);

    }

    public abstract static class AttributesPathAddressConfig<T extends AttributesPathAddressConfig<T>> implements PathAddressConfig {
        protected final Set<String> attributes;
        protected final Map<String, AttributesPathAddressConfig<?>> complexAttributes = new HashMap<String, AttributesPathAddressConfig<?>>();
        protected final Set<String> noWriteFailureAttributes = new HashSet<String>();
        protected final Set<String> readOnlyAttributes = new HashSet<String>();

        protected AttributesPathAddressConfig(String...attributes) {
            this.attributes = new HashSet<String>(Arrays.asList(attributes));
        }

        public AttributesPathAddressConfig<T> configureComplexAttribute(String attribute, T childConfig) {
            if (!attributes.contains(attribute)) {
                throw new IllegalStateException("Attempt to configure a complex attribute that was not set up as one of the original attributes");
            }
            complexAttributes.put(attribute, childConfig);
            return this;
        }

        protected static String[] convert(AttributeDefinition...defs) {
            String[] attrs = new String[defs.length];
            for (int i = 0 ; i < defs.length ; i++) {
                attrs[i] = defs[i].getName();
            }
            return attrs;
        }


        public List<ModelNode> createWriteAttributeOperations(ModelNode operation) {
            List<ModelNode> list = new ArrayList<ModelNode>();
            for (String attr : attributes) {
                if (operation.hasDefined(attr)) {
                    //TODO Should we also allow undefined here?

                    if (!readOnlyAttributes.contains(attr) && isAttributeWritable(attr)) {
                        list.add(Util.getWriteAttributeOperation(PathAddress.pathAddress(operation.get(OP_ADDR)), attr, operation.get(attr)));
                    }
                }
            }
            return list;
        }

        protected abstract boolean isAttributeWritable(String attributeName);

        public AttributesPathAddressConfig<T> setNotExpectedWriteFailure(String...attributes) {
            for (String attr : attributes) {
                noWriteFailureAttributes.add(attr);
            }
            return this;
        }

        public AttributesPathAddressConfig<T> setNotExpectedWriteFailure(AttributeDefinition...attributes) {
            for (AttributeDefinition attr : attributes) {
                noWriteFailureAttributes.add(attr.getName());
            }
            return this;
        }

        public AttributesPathAddressConfig<T> setReadOnly(String...attributes) {
            for (String attr : attributes) {
                readOnlyAttributes.add(attr);
            }
            return this;
        }

        public AttributesPathAddressConfig<T> setReadOnly(AttributeDefinition...attributes) {
            for (AttributeDefinition attr : attributes) {
                readOnlyAttributes.add(attr.getName());
            }
            return this;
        }

        @Override
        public boolean expectFailed(ModelNode operation) {
            ModelNode op = operation.clone();
            for (String attr : attributes) {
                if (checkValue(attr, op.get(attr), false)) {
                    return true;
                }
            }
            return false;
        }


        @Override
        public boolean canCorrectMore(ModelNode operation) {
            ModelNode op = operation.clone();
            for (String attr : attributes) {
                if (checkValue(attr, op.get(attr), false)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean expectFailedWriteAttributeOperation(ModelNode operation) {
            String name = operation.get(NAME).asString();
            if (attributes.contains(name)) {
                return !noWriteFailureAttributes.contains(name) && checkValue(name, operation.clone().get(VALUE), true);
            }
            return false;
        }

        @Override
        public ModelNode correctOperation(ModelNode operation) {
            ModelNode op = operation.clone();
            for (String attr : attributes) {
                ModelNode value = op.get(attr);
                if (value.isDefined()) {
                    if (checkValue(attr, value, false)) {
                        AttributesPathAddressConfig<?> complexChildConfig = complexAttributes.get(attr);
                        if (complexChildConfig == null) {
                            ModelNode resolved = correctValue(op.get(attr), false);
                            op.get(attr).set(resolved);
                        } else {
                            op.get(attr).set(complexChildConfig.correctOperation(operation.get(attr)));
                        }
                        return op;
                    }
                }
            }
            return operation;
        }

        @Override
        public ModelNode correctWriteAttributeOperation(ModelNode operation) {
            ModelNode op = operation.clone();
            String name = operation.get(NAME).asString();
            if (attributes.contains(name) && checkValue(name, op.get(VALUE), true)) {
                op.get(VALUE).set(correctValue(op.get(VALUE), true));
                return op;
            }
            return operation;
        }

        @Override
        public boolean expectDiscarded(ModelNode operation) {
            return false;
        }

        protected abstract boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute);

        protected abstract ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute);
    }

    /**
     * A standard configuration for the reject expression values transformer
     *
     */
    public static class RejectExpressionsConfig extends AttributesPathAddressConfig<RejectExpressionsConfig> {

        public RejectExpressionsConfig(String...attributes) {
            super(attributes);
        }

        public RejectExpressionsConfig(AttributeDefinition...attributes) {
            super(convert(attributes));
        }


        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            ModelNode result = toResolve.resolve();
            if (isWriteAttribute) {
                return result;
            }


            if (result.getType() == ModelType.STRING) {
                String rawVal = result.asString().toLowerCase();
                if ("true".equals(rawVal) || "false".equals(rawVal)) {
                    result.set(Boolean.valueOf(rawVal));
                } else {
                    try {
                        result.set(Integer.valueOf(rawVal));
                    } catch (Exception e) {
                        try {
                            result.set(Long.valueOf(rawVal));
                        } catch (Exception ignored) {
                            //
                        }
                    }
                }
            }
            return result;
        }


        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            if (!attribute.isDefined()) {
                return false;
            }

            AttributesPathAddressConfig<?> complexChildConfig = complexAttributes.get(attrName);
            switch (attribute.getType()) {
            case EXPRESSION:
                return true;
            case LIST:
                for (ModelNode entry : attribute.asList()) {
                    if (complexChildConfig == null) {
                        if (checkValue(attrName, entry, isWriteAttribute)) {
                            return true;
                        }
                    } else {
                        if (childHasExpressions(complexChildConfig, attribute.get(attrName), isWriteAttribute)) {
                            return true;
                        }
                    }
                }
                break;
            case OBJECT:
                for (Property prop : attribute.asPropertyList()) {
                    if (complexChildConfig == null) {
                        if (checkValue(attrName, prop.getValue(), isWriteAttribute)) {
                            return true;
                        }
                    } else {
                        if (childHasExpressions(complexChildConfig, attribute, isWriteAttribute)) {
                            return true;
                        }
                    }
                }
                break;
            case PROPERTY:
                if (complexChildConfig == null) {
                    if (checkValue(attrName, attribute.asProperty().getValue(), isWriteAttribute)) {
                        return true;
                    }
                } else {
                    if (childHasExpressions(complexChildConfig, attribute.asProperty().getValue(), isWriteAttribute)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean childHasExpressions(AttributesPathAddressConfig<?> complexChildConfig, ModelNode attribute, boolean isWriteAttribute) {
            for (String child : complexChildConfig.attributes) {
                if (complexChildConfig.checkValue(child, attribute.get(child), isWriteAttribute)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A standard configuration for the {@link DiscardUndefinedAttributesTransformer}
     * for use with attributes that are new in a version.
     */
    public static class NewAttributesConfig extends AttributesPathAddressConfig<NewAttributesConfig> {

        public NewAttributesConfig(String...attributes) {
            super(attributes);
        }

        public NewAttributesConfig(AttributeDefinition...attributes) {
            super(convert(attributes));
        }


        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return attribute.isDefined();
        }

        @Override
        protected ModelNode correctValue(ModelNode attribute, boolean isWriteAttribute) {
            return new ModelNode();
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }
    }

    /**
     * A standard configuration that allows several checkers to be used for an attribute.
     * For proper test coverage, the configs should be added in the same order as the rejecting transformers
     */
    public static class ChainedConfig extends AttributesPathAddressConfig<ChainedConfig> {

        private final List<AttributesPathAddressConfig<?>> list = new ArrayList<FailedOperationTransformationConfig.AttributesPathAddressConfig<?>>();

        /**
         * Constructor
         *
         * @param configs the configurations to use. For proper test coverage, these should be added in the same order as the rejecting transformers
         * @param attributes the attributes to apply these transformers to
         */
        public ChainedConfig(List<AttributesPathAddressConfig<?>> configs, String...attributes) {
            super(attributes);
            this.list.addAll(configs);
        }

        /**
         * Constructor
         *
         * @param configs the configurations to use. For proper test coverage, these should be added in the same order as the rejecting transformers
         * @param attributes the attributes to apply these transformers to
         */
        public ChainedConfig(List<AttributesPathAddressConfig<?>> configs, AttributeDefinition...attributes) {
            super(convert(attributes));
            this.list.addAll(configs);
        }

        @Override
        public boolean expectFailed(ModelNode operation) {
            for (AttributesPathAddressConfig<?> link : list) {
                if (link.expectFailed(operation)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public ModelNode correctOperation(ModelNode operation) {
            for (AttributesPathAddressConfig<?> link : list) {
                ModelNode op = link.correctOperation(operation);
                if (!op.equals(operation)) {
                    return op;
                }
            }
            return operation;
        }

        @Override
        public ModelNode correctWriteAttributeOperation(ModelNode operation) {
            ModelNode op = operation.clone();
            for (AttributesPathAddressConfig<?> link : list) {
                op = link.correctWriteAttributeOperation(op);
            }
            return op;
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        public boolean canCorrectMore(ModelNode operation) {
            for (AttributesPathAddressConfig<?> link : list) {
                if (link.canCorrectMore(operation)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean expectFailedWriteAttributeOperation(ModelNode operation) {
            if (!noWriteFailureAttributes.contains(operation.get(NAME).asString())) {
                for (AttributesPathAddressConfig<?> link : list) {
                    if (link.expectFailedWriteAttributeOperation(operation)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            //Since all the PathAddress methods have been overridden this should never be called
            throw new IllegalStateException("Not all methods were overridden");
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
          //Since all the PathAddress methods have been overridden this should never be called
            throw new IllegalStateException("Not all methods were overridden");
        }

        public interface Builder {
            Builder addConfig(AttributesPathAddressConfig<?> cfg);
            ChainedConfig build();
        }

        public static Builder createBuilder(final String...attributes) {
            return new Builder() {
                ArrayList<AttributesPathAddressConfig<?>> list = new ArrayList<FailedOperationTransformationConfig.AttributesPathAddressConfig<?>>();
                @Override
                public ChainedConfig build() {
                    return new ChainedConfig(list, attributes);
                }

                @Override
                public Builder addConfig(AttributesPathAddressConfig<?> cfg) {
                    list.add(cfg);
                    return this;
                }
            };
        }

        public static Builder createBuilder(AttributeDefinition...attributes) {
            return createBuilder(convert(attributes));
        }
    }

    public static final PathAddressConfig DISCARDED_RESOURCE = new PathAddressConfig() {

        @Override
        public boolean expectFailedWriteAttributeOperation(ModelNode operation) {
            throw new IllegalStateException("Should not get called");
        }

        @Override
        public boolean expectFailed(ModelNode operation) {
            return false;
        }

        @Override
        public boolean expectDiscarded(ModelNode operation) {
            return true;
        }

        @Override
        public List<ModelNode> createWriteAttributeOperations(ModelNode operation) {
            return Collections.emptyList();
        }

        @Override
        public ModelNode correctWriteAttributeOperation(ModelNode operation) {
            throw new IllegalStateException("Should not get called");
        }

        @Override
        public ModelNode correctOperation(ModelNode operation) {
            throw new IllegalStateException("Should not get called");
        }

        @Override
        public boolean canCorrectMore(ModelNode operation) {
            throw new IllegalStateException("Should not get called");
        }
    };

}
