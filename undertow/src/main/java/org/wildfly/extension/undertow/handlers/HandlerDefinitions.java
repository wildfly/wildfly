/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class HandlerDefinitions extends PersistentResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.CONFIGURATION, Constants.HANDLER);

    public HandlerDefinitions() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getValue()))
                .setAddHandler(ModelOnlyAddStepHandler.INSTANCE)
                .setRemoveHandler(ModelOnlyRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(
                new FileHandlerDefinition(),
                new ReverseProxyHandlerDefinition());
    }
}
