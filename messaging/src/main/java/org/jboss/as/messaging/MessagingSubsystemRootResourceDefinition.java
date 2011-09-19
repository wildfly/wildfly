/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Locale;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * {@link ResourceDefinition} for the messaging subsystem root resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    private static final PathElement SUBSYSTEM_PATH  = PathElement.pathElement(SUBSYSTEM, MessagingExtension.SUBSYSTEM_NAME);

    public static final MessagingSubsystemRootResourceDefinition INSTANCE = new MessagingSubsystemRootResourceDefinition();

    private MessagingSubsystemRootResourceDefinition() {
        super(SUBSYSTEM_PATH, MessagingExtension.getResourceDescriptionResolver(MessagingExtension.SUBSYSTEM_NAME),
                MessagingSubsystemAdd.INSTANCE, MessagingSubsystemRemove.INSTANCE);
    }
}
