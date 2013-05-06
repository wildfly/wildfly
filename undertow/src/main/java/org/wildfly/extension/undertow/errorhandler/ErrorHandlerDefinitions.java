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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class ErrorHandlerDefinitions extends SimplePersistentResourceDefinition {

    public static final ErrorHandlerDefinitions INSTANCE = new ErrorHandlerDefinitions();
    private static List<? extends SimplePersistentResourceDefinition> ERROR_HANDLERS = Collections.unmodifiableList(Arrays.asList(
            ErrorPageDefinition.INSTANCE,
            SimpleErrorPageDefinition.INSTANCE
    ));

    private ErrorHandlerDefinitions() {
        super(UndertowExtension.PATH_ERROR_HANDLERS,
                UndertowExtension.getResolver(Constants.ERROR_PAGE),
                new AbstractAddStepHandler(),
                ReloadRequiredRemoveStepHandler.INSTANCE
                );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public String getXmlElementName() {
        return "error-handlers";
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return ERROR_HANDLERS;
    }
}
