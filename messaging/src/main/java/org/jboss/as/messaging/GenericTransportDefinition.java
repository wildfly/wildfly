/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.dmr.ModelType.STRING;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;

/**
 * Generic transport resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class GenericTransportDefinition extends AbstractTransportDefinition {

    public static final SimpleAttributeDefinition SOCKET_BINDING = create("socket-binding", STRING)
            .setAllowNull(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    static AttributeDefinition[] ATTRIBUTES = { CommonAttributes.FACTORY_CLASS, SOCKET_BINDING };

    public static SimpleResourceDefinition createAcceptorDefinition(final boolean registerRuntimeOnly) {
        return new GenericTransportDefinition(registerRuntimeOnly, true, CommonAttributes.ACCEPTOR);
    }

    public static SimpleResourceDefinition createConnectorDefinition(final boolean registerRuntimeOnly) {
        return new GenericTransportDefinition(registerRuntimeOnly, false, CommonAttributes.CONNECTOR);
    }

    private GenericTransportDefinition(final boolean registerRuntimeOnly, boolean isAcceptor, String specificType) {
        super(registerRuntimeOnly, isAcceptor, specificType, ATTRIBUTES);
    }

    @Override
    protected Set<String> getAllowedKeys() {
        return Collections.emptySet();
    }
}
