/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 */
public enum PlainAuthTokenResourceDescription implements AuthTokenResourceDescription {
    INSTANCE;

    private final PathElement path = AuthTokenResourceDescription.pathElement("plain");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }
}
