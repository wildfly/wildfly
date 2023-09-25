/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentCreateServiceFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;

/**
 * User: jpai
 */
public abstract class EJBComponentCreateServiceFactory implements ComponentCreateServiceFactory {

    protected ApplicationExceptions ejbJarConfiguration;

    public void setEjbJarConfiguration(ApplicationExceptions ejbJarConfiguration) {
        if (ejbJarConfiguration == null) {
            throw EjbLogger.ROOT_LOGGER.EjbJarConfigurationIsNull();
        }
        this.ejbJarConfiguration = ejbJarConfiguration;
    }

}
