/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

/**
 * <p>
 * Defines a resource that encompasses all the settings that are to be applied when generating IORs.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class IORSettingsDefinition extends PersistentResourceDefinition {

    static final IORSettingsDefinition INSTANCE = new IORSettingsDefinition();

    private static final List<PersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(
            Arrays.asList(IORTransportConfigDefinition.INSTANCE, IORASContextDefinition.INSTANCE,
                    IORSASContextDefinition.INSTANCE));

    private IORSettingsDefinition() {
        super(PathElement.pathElement(JacORBSubsystemConstants.IOR_SETTINGS, JacORBSubsystemConstants.DEFAULT),
                JacORBExtension.getResourceDescriptionResolver(JacORBSubsystemConstants.IOR_SETTINGS),
                new ReloadRequiredAddStepHandler(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}
