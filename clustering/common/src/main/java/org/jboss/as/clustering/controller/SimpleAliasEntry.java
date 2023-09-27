/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Simple alias entry that converts address using the alias and target addresses with which it was initialized/registered.
 * @author Paul Ferraro
 */
public class SimpleAliasEntry extends AliasEntry {

    public SimpleAliasEntry(ManagementResourceRegistration registration) {
        super(registration);
    }

    @Override
    public PathAddress convertToTargetAddress(PathAddress address, AliasContext aliasContext) {
        PathAddress target = this.getTargetAddress();
        List<PathElement> result = new ArrayList<>(address.size());
        for (int i = 0; i < address.size(); ++i) {
            PathElement element = address.getElement(i);
            if (i < target.size()) {
                PathElement targetElement = target.getElement(i);
                result.add(targetElement.isWildcard() ? PathElement.pathElement(targetElement.getKey(), element.getValue()) : targetElement);
            } else {
                result.add(element);
            }
        }
        return PathAddress.pathAddress(result);
    }
}