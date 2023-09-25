/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

/**
 * A factory for component creation which allows component behavior customization.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ComponentCreateServiceFactory {

    /**
     * Construct a new component creation service from the given configuration.
     *
     * @param configuration the configuration
     * @return the create service
     */
    BasicComponentCreateService constructService(ComponentConfiguration configuration);

    /**
     * The default, basic component create service factory.
     */
    ComponentCreateServiceFactory BASIC = new ComponentCreateServiceFactory() {
        public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
            return new BasicComponentCreateService(configuration);
        }
    };
}
