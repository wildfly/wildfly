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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.MessagingExtension.SECURITY_SETTING_ACCESS_CONSTRAINT;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SECURITY_SETTING_PATH;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;

/**
 * Security setting resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class SecuritySettingDefinition extends PersistentResourceDefinition {

    private static final PersistentResourceDefinition[] CHILDREN = {
            SecurityRoleDefinition.INSTANCE
    };

    static final SecuritySettingDefinition INSTANCE = new SecuritySettingDefinition();

    private SecuritySettingDefinition() {
        super(SECURITY_SETTING_PATH,
                MessagingExtension.getResourceDescriptionResolver(false, SECURITY_SETTING_PATH.getKey()),
                SecuritySettingAdd.INSTANCE,
                SecuritySettingRemove.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Arrays.asList(CHILDREN);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Arrays.asList(SECURITY_SETTING_ACCESS_CONSTRAINT);
    }
}
