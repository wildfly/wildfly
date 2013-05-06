/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow.errorhandler;

import java.util.Collection;
import java.util.Collections;

import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.AbstractHandlerDefinition;
import org.wildfly.extension.undertow.Constants;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class SimpleErrorPageDefinition extends AbstractHandlerDefinition {

    static final SimpleErrorPageDefinition INSTANCE = new SimpleErrorPageDefinition();
    private static final AttributeDefinition CODE = new SimpleAttributeDefinitionBuilder("code", ModelType.INT)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

    private SimpleErrorPageDefinition() {
        super(Constants.SIMPLE_ERROR_PAGE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singleton(CODE);
    }

    @Override
    public Class getHandlerClass() {
        return SimpleErrorPageHandler.class;
    }
}
