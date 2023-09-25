/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.saml;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class SAMLResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition TOKEN_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelElement.SAML_TOKEN_TIMEOUT.getName(), ModelType.INT, true)
        .setDefaultValue(new ModelNode(5000))
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition CLOCK_SKEW = new SimpleAttributeDefinitionBuilder(ModelElement.SAML_CLOCK_SKEW.getName(), ModelType.INT, true)
        .setDefaultValue(ModelNode.ZERO)
        .setAllowExpression(true)
        .build();

    public static final SAMLResourceDefinition INSTANCE = new SAMLResourceDefinition();

    private SAMLResourceDefinition() {
        super(ModelElement.SAML, ModelElement.SAML.getName(), new ModelOnlyAddStepHandler(TOKEN_TIMEOUT, CLOCK_SKEW), TOKEN_TIMEOUT, CLOCK_SKEW);
    }


}
