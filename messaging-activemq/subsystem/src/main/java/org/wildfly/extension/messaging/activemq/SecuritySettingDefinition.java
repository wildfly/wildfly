/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

    SecuritySettingDefinition() {
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
        return List.of(new SecurityRoleDefinition(false));
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Arrays.asList(SECURITY_SETTING_ACCESS_CONSTRAINT);
    }
}
