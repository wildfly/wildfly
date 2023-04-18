/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * Resource description for the affinity cookie configuration with cookie name being required and no comment being defined.
 *
 * @author Radoslav Husar
 */
class AffinityCookieDefinition extends AbstractCookieDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.AFFINITY_COOKIE);

    static final Collection<AttributeDefinition> ATTRIBUTES = EnumSet.complementOf(EnumSet.of(Attribute.OPTIONAL_NAME, Attribute.COMMENT))
            .stream().map(Attribute::getDefinition).collect(Collectors.toUnmodifiableSet());

    AffinityCookieDefinition() {
        super(PATH_ELEMENT, ATTRIBUTES);
    }

    static CookieConfig getConfig(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
        return AbstractCookieDefinition.getConfig(Attribute.REQUIRED_NAME, context, model);
    }

}
