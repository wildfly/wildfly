/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;

/**
 * Descriptions of JDBC protocol resources.
 * @author Paul Ferraro
 */
public enum JDBCProtocolResourceDescription implements ProtocolResourceDescription {
    JDBC_PING;

    static final CapabilityReferenceAttributeDefinition<DataSource> DATA_SOURCE = new CapabilityReferenceAttributeDefinition.Builder<>("data-source", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.DATA_SOURCE).build()).build();

    private final PathElement path = ProtocolResourceDescription.pathElement(this.name());

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(Stream.of(DATA_SOURCE), ProtocolResourceDescription.super.getAttributes());
    }
}
