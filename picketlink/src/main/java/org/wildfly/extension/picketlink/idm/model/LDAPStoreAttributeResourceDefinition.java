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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class LDAPStoreAttributeResourceDefinition extends AbstractIDMResourceDefinition {

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_ATTRIBUTE_NAME.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition LDAP_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_ATTRIBUTE_LDAP_NAME.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition IS_IDENTIFIER = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_ATTRIBUTE_IS_IDENTIFIER.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(new ModelNode(false))
        .setAlternatives(ModelElement.LDAP_STORE_ATTRIBUTE_READ_ONLY.getName())
        .build();
    public static final SimpleAttributeDefinition READ_ONLY = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_ATTRIBUTE_READ_ONLY.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(new ModelNode(false))
        .setAlternatives(ModelElement.LDAP_STORE_ATTRIBUTE_IS_IDENTIFIER.getName())
        .build();
    public static final LDAPStoreAttributeResourceDefinition INSTANCE = new LDAPStoreAttributeResourceDefinition(NAME, LDAP_NAME, IS_IDENTIFIER, READ_ONLY);

    private LDAPStoreAttributeResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.LDAP_STORE_ATTRIBUTE, new IDMConfigAddStepHandler(attributes) {
            @Override
            protected boolean isAlternativesRequired() {
                return false;
            }
        }, attributes);
    }
}
