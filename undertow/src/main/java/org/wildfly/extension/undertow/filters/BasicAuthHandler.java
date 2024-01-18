/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.Collections;

import io.undertow.security.handlers.AuthenticationCallHandler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * TODO: This appears to be useless, we should probably get rid of it, or replace it with something useful
 *
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
@Deprecated
public class BasicAuthHandler extends SimpleFilterDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement("basic-auth");

    public static final AttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder("security-domain", ModelType.STRING)
            .setRequired(true)
            .setRestartAllServices()
            .build();

    private BasicAuthHandler() {
        super(PATH_ELEMENT, BasicAuthHandler::createHandlerWrapper);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singleton(SECURITY_DOMAIN);
    }

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) {
        return PredicateHandlerWrapper.filter(AuthenticationCallHandler::new);
    }
}
