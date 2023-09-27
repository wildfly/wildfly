/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.messagedriven;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentCreateServiceFactory;

/**
 * User: jpai
 */
public class MessageDrivenComponentCreateServiceFactory extends EJBComponentCreateServiceFactory {
    private final Class<?> messageListenerInterface;

    public MessageDrivenComponentCreateServiceFactory(final Class<?> messageListenerInterface) {
        this.messageListenerInterface = messageListenerInterface;
    }

    @Override
    public BasicComponentCreateService constructService(ComponentConfiguration configuration) {
        if (this.ejbJarConfiguration == null) {
            throw EjbLogger.ROOT_LOGGER.ejbJarConfigNotBeenSet(this, configuration.getComponentName());
        }
        return new MessageDrivenComponentCreateService(configuration, this.ejbJarConfiguration, messageListenerInterface);
    }
}
