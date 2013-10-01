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

package org.jboss.as.naming.subsystem;

import static org.jboss.as.naming.NamingMessages.MESSAGES;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NameParser;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.naming.management.JndiViewOperation;
import org.jboss.as.naming.util.DefaultNameParserResolver;
import org.jboss.as.naming.util.NameParserResolver;
import org.jboss.as.naming.util.RmiNameParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the Naming subsystem's root management resource.
 *
 * @author Stuart Douglas
 */
public class NamingSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    static final SimpleOperationDefinition JNDI_VIEW = new SimpleOperationDefinitionBuilder(JndiViewOperation.OPERATION_NAME, NamingExtension.getResourceDescriptionResolver(NamingExtension.SUBSYSTEM_NAME))
    .addAccessConstraint(NamingExtension.JNDI_VIEW_CONSTRAINT)
    .withFlag(OperationEntry.Flag.RUNTIME_ONLY)
    .build();

    static final SimpleAttributeDefinition RESOLVER_CLASS = new SimpleAttributeDefinitionBuilder(
            NamingSubsystemModel.RESOLVER_CLASS, ModelType.STRING)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setAllowNull(true)
            .setAllowExpression(false)
            .setValidator(new ClassValidator(NameParserResolver.class))
            .setDefaultValue(new ModelNode().set(DefaultNameParserResolver.class.getName())).build();

    static final SimpleMapAttributeDefinition RESOLVER_MAPPING;
    static {

        Map<String,String> resolverMappingDefaultValue = new HashMap<String, String>();
        resolverMappingDefaultValue.put(NameParserResolver.KEY_DEFAULT,org.jboss.as.naming.util.NameParser.class.getName());
        resolverMappingDefaultValue.put(NameParserResolver.KEY_RMI,RmiNameParser.class.getName());
        RESOLVER_MAPPING = new SimpleMapAttributeDefinition.Builder(NamingSubsystemModel.RESOLVER_MAPPING, false)
                .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                .setAllowNull(true)
                .setAllowExpression(false)
                .setValidator(new ClassValidator(NameParser.class))
                .setDefaultValue(resolverMappingDefaultValue)
                .setAttributeMarshaller(new DefaultAttributeMarshaller() {
                    @Override
                    public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel,
                            boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                        resourceModel = resourceModel.get(attribute.getName());
                        if (resourceModel.isDefined()) {
                            writer.writeStartElement(attribute.getName());
                            for (ModelNode property : resourceModel.asList()) {
                                writer.writeEmptyElement("mapping");
                                writer.writeAttribute("value", property.asProperty().getName());
                                writer.writeAttribute("class", property.asProperty().getValue().asString());
                            }
                            writer.writeEndElement();
                        }
                    }
                }).build();
    }
    //this must be last, so other statics are initialized before constructor is called
    public static final NamingSubsystemRootResourceDefinition INSTANCE = new NamingSubsystemRootResourceDefinition();

    private NamingSubsystemRootResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME), NamingExtension
                .getResourceDescriptionResolver(NamingExtension.SUBSYSTEM_NAME), NamingSubsystemAdd.INSTANCE,
                NamingSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(RESOLVER_CLASS, null, new ReloadRequiredWriteAttributeHandler(RESOLVER_CLASS));
        resourceRegistration.registerReadWriteAttribute(RESOLVER_MAPPING, null, new ReloadRequiredWriteAttributeHandler(RESOLVER_MAPPING));
    }

    @SuppressWarnings("all")
    private static class ClassValidator implements ParameterValidator {

        private Class requiredInterface;

        public ClassValidator(Class requiredInterface) {
            super();
            this.requiredInterface = requiredInterface;
        }
        @Override
        public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if(value.isDefined()){
                    validateClass(value.asString());
            } else {
                throw MESSAGES.attributeMustBeDefined(parameterName);
            }
        }
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            this.validateResolvedParameter(parameterName, value.resolve());
        }

        protected void validateClass(final String className) throws OperationFailedException {
            Class clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw MESSAGES.cannottLoadClass(className,e);
            }
            if (!requiredInterface.isAssignableFrom(clazz)) {
                throw MESSAGES.requireSpecificInterface(className, requiredInterface.getName());
            }
            try {
                clazz.getConstructor(null);
            } catch (NoSuchMethodException nsme) {
                throw MESSAGES.requireNoArgConstructor(className);
            } catch (SecurityException se) {
                throw MESSAGES.requireNoArgConstructor(className);
            }
        }
    }
}
