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

package org.jboss.as.domain.management.connections.ldap;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.operations.validation.URIValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a connection factory for an LDAP-based security store.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LdapConnectionResourceDefinition extends SimpleResourceDefinition {


    public static final PathElement RESOURCE_PATH = PathElement.pathElement(LDAP_CONNECTION);

    private static final String DEFAULT_INITIAL_CONTEXT = "com.sun.jndi.ldap.LdapCtxFactory";

    public static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URL, ModelType.STRING, false)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .build();

    public static final SimpleAttributeDefinition SEARCH_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SEARCH_DN, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();

    public static final SimpleAttributeDefinition SEARCH_CREDENTIAL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SEARCH_CREDENTIAL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, true))
            .build();

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURITY_REALM, ModelType.STRING, true)
            .setAllowExpression(false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();

    public static final SimpleAttributeDefinition INITIAL_CONTEXT_FACTORY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(DEFAULT_INITIAL_CONTEXT))
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();

    public static final SimpleAttributeDefinition REFERRALS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REFERRALS, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(ReferralHandling.IGNORE.toString()))
            .setValidator(new EnumValidator<>(ReferralHandling.class, true, true))
            .build();

    public static final StringListAttributeDefinition HANDLES_REFERRALS_FOR = new StringListAttributeDefinition.Builder(ModelDescriptionConstants.HANDLES_REFERRALS_FOR)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setValidator(new URIValidator(true, true))
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    if (resourceModel.hasDefined(attribute.getName())) {
                        List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                        if (list.size() > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (ModelNode child : list) {
                                if (sb.length() > 0) {
                                    sb.append(" ");
                                }
                                sb.append(child.asString());
                            }
                            writer.writeAttribute(Attribute.HANDLES_REFERRALS_FOR.getLocalName(), sb.toString());
                        }
                    }
                }
            })
            .build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {URL, SEARCH_DN, SEARCH_CREDENTIAL, SECURITY_REALM, INITIAL_CONTEXT_FACTORY, REFERRALS, HANDLES_REFERRALS_FOR};



    private LdapConnectionResourceDefinition(OperationStepHandler add, OperationStepHandler remove) {
        super(RESOURCE_PATH, ControllerResolver.getResolver("core.management.ldap-connection"),
                add, remove,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    public static LdapConnectionResourceDefinition newInstance() {
        LdapConnectionAddHandler add = LdapConnectionAddHandler.newInstance();
        LdapConnectionRemoveHandler remove = LdapConnectionRemoveHandler.newInstance(add);
        return new LdapConnectionResourceDefinition(add, remove);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(LdapConnectionPropertyResourceDefinition.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        LdapConnectionWriteAttributeHandler writeHandler = new LdapConnectionWriteAttributeHandler();
        writeHandler.registerAttributes(resourceRegistration);
    }

    public enum ReferralHandling {

        FOLLOW(ModelDescriptionConstants.FOLLOW),
        IGNORE(ModelDescriptionConstants.IGNORE),
        THROW(ModelDescriptionConstants.THROW);

        private final String value;

        private ReferralHandling(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
