/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 20:04
 */
class MailSubsystemDefinition extends PersistentResourceDefinition {

    MailSubsystemDefinition() {
        super(MailExtension.SUBSYSTEM_PATH,
                MailExtension.getResourceDescriptionResolver(),
                new MailSubsystemAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(new MailSessionDefinition());
    }
}
