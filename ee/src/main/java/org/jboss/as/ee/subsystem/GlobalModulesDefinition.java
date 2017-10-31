/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.subsystem;

import java.util.ArrayList;
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
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.ModuleIdentifier;

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
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final SimpleAttributeDefinition SERVICES_AD = new SimpleAttributeDefinitionBuilder(SERVICES, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final SimpleAttributeDefinition META_INF_AD  = new SimpleAttributeDefinitionBuilder(META_INF, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
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
                    valueType.getAttributeMarshaller().marshall(valueType, resourceModel, true, writer);
                }
            }
        }
    };

    private static final ObjectTypeAttributeDefinition VALUE_TYPE_AD =
            ObjectTypeAttributeDefinition.Builder.of(MODULE, VALUE_TYPE_FIELDS)
                    .setAttributeMarshaller(VALUE_TYPE_MARSHALLER)
                    .build();

    public static final AttributeDefinition INSTANCE = ObjectListAttributeDefinition.Builder.of(GLOBAL_MODULES, VALUE_TYPE_AD)
        .setRequired(false)
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
                ret.add(new GlobalModule(ModuleIdentifier.create(name, slot), annotations, services, metaInf));
            }
        }
        return ret;
    }

    public static final class GlobalModule {
        private final ModuleIdentifier moduleIdentifier;
        private final boolean annotations;
        private final boolean services;
        private final boolean metaInf;


        GlobalModule(final ModuleIdentifier moduleIdentifier, final boolean annotations, final boolean services, final boolean metaInf) {
            this.moduleIdentifier = moduleIdentifier;
            this.annotations = annotations;
            this.services = services;
            this.metaInf = metaInf;
        }

        public ModuleIdentifier getModuleIdentifier() {
            return moduleIdentifier;
        }

        public boolean isAnnotations() {
            return annotations;
        }

        public boolean isServices() {
            return services;
        }

        public boolean isMetaInf() {
            return metaInf;
        }
    }
}
