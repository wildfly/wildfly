/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Resource description for the affinity cookie configuration with cookie name being required and no comment being defined.
 *
 * @author Radoslav Husar
 */
class AffinityCookieDefinition extends AbstractCookieDefinition {
    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement(Constants.SETTING, Constants.AFFINITY_COOKIE));

    static final Collection<AttributeDefinition> ATTRIBUTES = EnumSet.complementOf(EnumSet.of(Attribute.OPTIONAL_NAME, Attribute.COMMENT))
            .stream().map(Attribute::getDefinition).collect(Collectors.toUnmodifiableSet());

    AffinityCookieDefinition() {
        super(REGISTRATION, ATTRIBUTES);
    }

    static CookieConfig getConfig(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
        return AbstractCookieDefinition.getConfig(Attribute.REQUIRED_NAME, context, model);
    }

}
