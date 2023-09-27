/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.managedbean.component;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.ComponentTypeInjectionSource;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * User: jpai
 */
public class ManagedBeanResourceReferenceProcessor implements EEResourceReferenceProcessor {

    private final String managedBeanClassName;

    public ManagedBeanResourceReferenceProcessor(final String managedBeanClassName) {
        if (managedBeanClassName == null || managedBeanClassName.trim().isEmpty()) {
            throw EeLogger.ROOT_LOGGER.nullOrEmptyManagedBeanClassName();
        }
        this.managedBeanClassName = managedBeanClassName;
    }

    @Override
    public String getResourceReferenceType() {
        return this.managedBeanClassName;
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        ROOT_LOGGER.debugf("Processing @Resource of type: %s", this.managedBeanClassName);
        // ComponentType binding source for managed beans
        final InjectionSource bindingSource = new ComponentTypeInjectionSource(this.managedBeanClassName);
        return bindingSource;
    }
}
