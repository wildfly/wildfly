/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleResourceDefinition;

import java.util.Collection;
import java.util.List;

/**
 * Global welcome file definition
 *
 * @author Stuart Douglas
 */
class WelcomeFileDefinition extends PersistentResourceDefinition {
    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement(Constants.WELCOME_FILE));
    static final PathElement PATH_ELEMENT = REGISTRATION.getPathElement();

    WelcomeFileDefinition() {
        super(new SimpleResourceDefinition.Parameters(REGISTRATION, UndertowRootDefinition.RESOLVER.createChildResolver(REGISTRATION.getPathElement()))
                .setAddHandler(ReloadRequiredAddStepHandler.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return List.of();
    }
}
