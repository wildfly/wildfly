/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.handlers;

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
public class HandlerParameterResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_VALUE.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();

    public static final HandlerParameterResourceDefinition INSTANCE = new HandlerParameterResourceDefinition();

    private HandlerParameterResourceDefinition() {
        super(ModelElement.COMMON_HANDLER_PARAMETER, new ModelOnlyAddStepHandler(VALUE), VALUE);
    }
}
