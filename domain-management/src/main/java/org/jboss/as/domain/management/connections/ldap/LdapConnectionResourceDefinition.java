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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a connection factory for an LDAP-based security store.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LdapConnectionResourceDefinition extends SimpleResourceDefinition {


    public static final PathElement RESOURCE_PATH = PathElement.pathElement(LDAP_CONNECTION);

    private static final String DEFAULT_INITIAL_CONTEXT = "com.sun.jndi.ldap.LdapCtxFactory";

    public static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URL, ModelType.STRING, false)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true)).build();

    public static final SimpleAttributeDefinition SEARCH_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SEARCH_DN, ModelType.STRING, false)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true)).build();

    public static final SimpleAttributeDefinition SEARCH_CREDENTIAL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SEARCH_CREDENTIAL, ModelType.STRING, false)
            .setAllowExpression(true).setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, false, true)).build();

    public static final SimpleAttributeDefinition INITIAL_CONTEXT_FACTORY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INITIAL_CONTEXT_FACTORY, ModelType.STRING, true)
            .setAllowExpression(true).setDefaultValue(new ModelNode(DEFAULT_INITIAL_CONTEXT)).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true)).build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {URL, SEARCH_DN, SEARCH_CREDENTIAL, INITIAL_CONTEXT_FACTORY};

    public static final LdapConnectionResourceDefinition INSTANCE = new LdapConnectionResourceDefinition();

    private LdapConnectionResourceDefinition() {
        super(RESOURCE_PATH, CommonDescriptions.getResourceDescriptionResolver("core.management.ldap-connection"),
                LdapConnectionAddHandler.INSTANCE, LdapConnectionRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        LdapConnectionWriteAttributeHandler writeHandler = new LdapConnectionWriteAttributeHandler();
        writeHandler.registerAttributes(resourceRegistration);
    }
}
