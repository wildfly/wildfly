/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;

/**
 * @author Paul Ferraro
 *
 */
public enum SocketTransportResourceDescription implements TransportResourceDescription {
    TCP,
    TCP_NIO2,
    ;
    private final PathElement path = TransportResourceDescription.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(TransportResourceDescription.super.getAttributes(), Stream.of(TransportResourceDescription.SocketBindingAttribute.CLIENT.get()));
    }
}
