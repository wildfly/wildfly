/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.keystore;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class KeyResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition HOST = new SimpleAttributeDefinitionBuilder(ModelElement.HOST.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();

    public static final KeyResourceDefinition INSTANCE = new KeyResourceDefinition();

    private KeyResourceDefinition() {
        super(ModelElement.KEY, new ModelOnlyAddStepHandler(HOST), HOST);
    }
}
