/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;

/**
 * @author Paul Ferraro
 */
public abstract class TimerPersistenceResourceDefinition extends SimpleResourceDefinition {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(TimerPersistence.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    TimerPersistenceResourceDefinition(Parameters parameters) {
        super(parameters.addCapabilities(CAPABILITY));
    }
}
