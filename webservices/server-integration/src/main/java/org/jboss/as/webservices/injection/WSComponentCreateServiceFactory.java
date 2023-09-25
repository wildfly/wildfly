/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.injection;

import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentCreateServiceFactory;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSComponentCreateServiceFactory implements ComponentCreateServiceFactory {

    static final WSComponentCreateServiceFactory INSTANCE = new WSComponentCreateServiceFactory();

    private WSComponentCreateServiceFactory() {}

    @Override
    public BasicComponentCreateService constructService(final ComponentConfiguration configuration) {
        return new WSComponentCreateService(configuration);
    }
}
