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

package org.jboss.as.messaging;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Security setting resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class SecuritySettingDefinition extends SimpleResourceDefinition {

    private final boolean registerRuntimeOnly;
    private final List<AccessConstraintDefinition> accessConstraints;

    public SecuritySettingDefinition(final boolean registerRuntimeOnly) {
        super(PathElement.pathElement(CommonAttributes.SECURITY_SETTING),
                MessagingExtension.getResourceDescriptionResolver(false, CommonAttributes.SECURITY_SETTING),
                SecuritySettingAdd.INSTANCE,
                SecuritySettingRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(MessagingExtension.SUBSYSTEM_NAME, CommonAttributes.SECURITY_SETTING);
        AccessConstraintDefinition acd = new ApplicationTypeAccessConstraintDefinition(atc);
        this.accessConstraints = Arrays.asList((AccessConstraintDefinition) CommonAttributes.MESSAGING_SECURITY_DEF, acd);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        super.registerChildren(registry);

        registry.registerSubModel(SecurityRoleDefinition.newSecurityRoleDefinition(registerRuntimeOnly));
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
