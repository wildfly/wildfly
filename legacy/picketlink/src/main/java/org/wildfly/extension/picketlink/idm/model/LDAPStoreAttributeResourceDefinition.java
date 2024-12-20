/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        .setDefaultValue(ModelNode.FALSE)
        .setAlternatives(ModelElement.LDAP_STORE_ATTRIBUTE_READ_ONLY.getName())
        .build();
    public static final SimpleAttributeDefinition READ_ONLY = new SimpleAttributeDefinitionBuilder(ModelElement.LDAP_STORE_ATTRIBUTE_READ_ONLY.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.FALSE)
        .setAlternatives(ModelElement.LDAP_STORE_ATTRIBUTE_IS_IDENTIFIER.getName())
        .build();
    public static final LDAPStoreAttributeResourceDefinition INSTANCE = new LDAPStoreAttributeResourceDefinition(NAME, LDAP_NAME, IS_IDENTIFIER, READ_ONLY);

    private LDAPStoreAttributeResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.LDAP_STORE_ATTRIBUTE, address->address.getParent().getParent().getParent().getParent(), attributes);
    }
}
