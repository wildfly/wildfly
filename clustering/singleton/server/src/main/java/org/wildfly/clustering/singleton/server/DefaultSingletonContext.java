/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.server.service.Service;

/**
 * @author Paul Ferraro
 */
public class DefaultSingletonContext extends AbstractSingletonContext<SingletonContext, Service> {

    public DefaultSingletonContext(SingletonServiceContext context, Service service) {
        super(context, service);
    }

    @Override
    public SingletonContext get() {
        return this;
    }
}
