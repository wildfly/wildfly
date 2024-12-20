/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class SupportedTypesResourceDefinition extends AbstractIDMResourceDefinition {

    public static final SimpleAttributeDefinition SUPPORTS_ALL = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_SUPPORTS_ALL.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(ModelNode.FALSE)
        .setAllowExpression(true)
        .build();
    public static final SupportedTypesResourceDefinition INSTANCE = new SupportedTypesResourceDefinition(SUPPORTS_ALL);

    private SupportedTypesResourceDefinition(SimpleAttributeDefinition... attributes) {
        super(ModelElement.SUPPORTED_TYPES, address -> address.getParent().getParent().getParent(), attributes);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(SupportedTypeResourceDefinition.INSTANCE, resourceRegistration);
    }
}
