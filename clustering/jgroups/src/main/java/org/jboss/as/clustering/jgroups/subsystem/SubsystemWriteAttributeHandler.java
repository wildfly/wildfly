/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Handler for read and write access to the subsystem attribute default-stack.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SubsystemWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final SubsystemWriteAttributeHandler INSTANCE = new SubsystemWriteAttributeHandler();

    private SubsystemWriteAttributeHandler() {
        super(JGroupsSubsystemRootResource.DEFAULT_STACK);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadWriteAttribute(JGroupsSubsystemRootResource.DEFAULT_STACK, null, this);
    }
}
