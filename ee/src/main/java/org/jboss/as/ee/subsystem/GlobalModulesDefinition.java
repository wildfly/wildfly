/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import static org.jboss.as.controller.ModuleIdentifierUtil.canonicalModuleIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link AttributeDefinition} implementation for the "global-modules" attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class GlobalModulesDefinition {

    public static final String NAME = "name";
    public static final String SLOT = "slot";
    public static final String ANNOTATIONS = "annotations";
    public static final String META_INF = "meta-inf";
    public static final String SERVICES = "services";
    public static final String GLOBAL_MODULES = "global-modules";

    public static final String DEFAULT_SLOT = "main";

    public static final SimpleAttributeDefinition NAME_AD = new SimpleAttributeDefinitionBuilder(NAME, ModelType.STRING).build();

    public static final SimpleAttributeDefinition SLOT_AD = new SimpleAttributeDefinitionBuilder(SLOT, ModelType.STRING, true)
            .setDefaultValue(new ModelNode(DEFAULT_SLOT))
            .build();

    public static final SimpleAttributeDefinition ANNOTATIONS_AD = new SimpleAttributeDefinitionBuilder(ANNOTATIONS, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    public static final SimpleAttributeDefinition SERVICES_AD = new SimpleAttributeDefinitionBuilder(SERVICES, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    public static final SimpleAttributeDefinition META_INF_AD  = new SimpleAttributeDefinitionBuilder(META_INF, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .build();

    private static final SimpleAttributeDefinition[] VALUE_TYPE_FIELDS = { NAME_AD, SLOT_AD, ANNOTATIONS_AD, SERVICES_AD, META_INF_AD };

    // TODO the default marshalling in ObjectListAttributeDefinition is not so great since it delegates each
    // element to ObjectTypeAttributeDefinition, and OTAD assumes it's used for complex attributes bound in a
    // ModelType.OBJECT node under key=OTAD.getName(). So provide a custom marshaller to OTAD. This could be made reusable.
    private static final AttributeMarshaller VALUE_TYPE_MARSHALLER = new AttributeMarshaller() {
        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.isDefined()) {
                writer.writeEmptyElement(Element.MODULE.getLocalName());
                for (SimpleAttributeDefinition valueType : VALUE_TYPE_FIELDS) {
                    valueType.getMarshaller().marshall(valueType, resourceModel, true, writer);
                }
            }
        }
    };

    private static final ObjectTypeAttributeDefinition VALUE_TYPE_AD =
            ObjectTypeAttributeDefinition.Builder.of(MODULE, VALUE_TYPE_FIELDS)
                    .setAttributeMarshaller(VALUE_TYPE_MARSHALLER)
                    .build();

    private static final ParameterCorrector DUPLICATE_CORRECTOR = new ParameterCorrector() {

        @Override
        public ModelNode correct(ModelNode newValue, ModelNode currentValue) {

            ModelNode result = null;
            if (newValue.isDefined()) {
                ArrayList<ModelNode> elementSet = new ArrayList<>();
                LinkedHashSet<String> identifierSet = new LinkedHashSet<>();

                List<ModelNode> asList = newValue.asList();
                for (int i = asList.size() -1; i >= 0; i--) {
                    ModelNode element = asList.get(i);
                    ModelNode name = element.get(GlobalModulesDefinition.NAME);
                    ModelNode slot = element.get(GlobalModulesDefinition.SLOT);

                    if (!identifierSet.add(name + ":" + slot)) {
                        // Leave this at debug for now. WFCORE-5070 may add a formalized i18n log message in WFLYCTL
                        EeLogger.ROOT_LOGGER.debugf("Removing duplicate entry %s from %s attribute %s", element.toString(), GLOBAL_MODULES);
                    } else {
                        elementSet.add(element);
                    }
                }

                if (!elementSet.isEmpty()) {
                    Collections.reverse(elementSet);
                    result = new ModelNode();
                    for (ModelNode element : elementSet) {
                        result.add(element);
                    }
                }
            }
            return result == null ? newValue : result;
        }
    };

    public static final AttributeDefinition INSTANCE = ObjectListAttributeDefinition.Builder.of(GLOBAL_MODULES, VALUE_TYPE_AD)
        .setRequired(false)
        .setCorrector(DUPLICATE_CORRECTOR)
        .build();

    public static List<GlobalModule> createModuleList(final OperationContext context, final ModelNode globalMods) throws OperationFailedException {
        final List<GlobalModule> ret = new ArrayList<>();
        if (globalMods.isDefined()) {
            for (final ModelNode module : globalMods.asList()) {
                String name = NAME_AD.resolveModelAttribute(context, module).asString();
                String slot = SLOT_AD.resolveModelAttribute(context, module).asString();
                boolean annotations = ANNOTATIONS_AD.resolveModelAttribute(context, module).asBoolean();
                boolean services = SERVICES_AD.resolveModelAttribute(context, module).asBoolean();
                boolean metaInf = META_INF_AD.resolveModelAttribute(context, module).asBoolean();
                ret.add(new GlobalModule(canonicalModuleIdentifier(name, slot), annotations, services, metaInf));
            }
        }
        return ret;
    }

    /**
     * Descriptive information for a module that should be added as a dependency to all deployment modules.
     */
    public static final class GlobalModule {
        private final String moduleName;
        private final boolean annotations;
        private final boolean services;
        private final boolean metaInf;


        /**
         * Creates a new global module.
         *
         * @param moduleName {@link org.jboss.as.controller.ModuleIdentifierUtil#canonicalModuleIdentifier(String) canonicalized} name of the module
         * @param annotations {@code true} if the module should be indexed for annotations
         * @param services {@code true} if dependent modules should be able to import services from this module
         * @param metaInf {@code true} if dependent modules should be able to import META-INF resources from this module
         */
        GlobalModule(final String moduleName, final boolean annotations, final boolean services, final boolean metaInf) {
            this.moduleName = moduleName;
            this.annotations = annotations;
            this.services = services;
            this.metaInf = metaInf;
        }

        /**
         * Gets the name of the module.
         *
         * @return the {@link org.jboss.as.controller.ModuleIdentifierUtil#canonicalModuleIdentifier(String) canonicalized} name of the module
         *         Will not return {@code null}
         */
        public String getModuleName() {
            return moduleName;
        }

        /**
         * Gets whether the module should be indexed for annotations.
         * @return {@code true} if the module should be indexed for annotations
         */
        public boolean isAnnotations() {
            return annotations;
        }

        /**
         * Gets whether dependent modules should be able to import services from this module
         * @return {@code true} if dependent modules should be able to import services from this module
         */
        public boolean isServices() {
            return services;
        }

        /**
         * Gets whether dependent modules should be able to import META-INF resources from this module
         * @return {@code true} if dependent modules should be able to import META-INF resources from this module
         */
        public boolean isMetaInf() {
            return metaInf;
        }
    }
}
