/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class FilterDefinitions extends PersistentResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.CONFIGURATION, Constants.FILTER);

    public FilterDefinitions() {
        super(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getValue()),
                new ModelOnlyAddStepHandler(),
                ModelOnlyRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(
                new RequestLimitHandlerDefinition(),
                new ResponseHeaderFilterDefinition(),
                new GzipFilterDefinition(),
                new ErrorPageDefinition(),
                new CustomFilterDefinition(),
                new ModClusterDefinition(),
                new ExpressionFilterDefinition(),
                new RewriteFilterDefinition());
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        PathElement targetPe = RequestLimitHandlerDefinition.PATH_ELEMENT;
        AliasEntry aliasEntry = new AliasEntry(resourceRegistration.getSubModel(PathAddress.pathAddress(targetPe))) {
            @Override
            public PathAddress convertToTargetAddress(PathAddress aliasAddress, AliasContext aliasContext) {
                PathElement pe = aliasAddress.getLastElement();

                return aliasAddress.getParent().append(PathElement.pathElement(targetPe.getKey(), pe.getValue()));
            }
        };
        resourceRegistration.registerAlias(PathElement.pathElement("connection-limit"), aliasEntry);
    }
}
