/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
                ModelOnlyAddStepHandler.INSTANCE,
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
