/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming;

import javax.naming.spi.ObjectFactory;

import org.jboss.msc.service.ServiceRegistry;

/**
 * Interface the should be implemented by {@link javax.naming.spi.ObjectFactory}s that require access to the {@link ServiceRegistry}.
 * <p>
 * After the object is created the {@link org.jboss.as.naming.context.ObjectFactoryBuilder} will inject the {@link ServiceRegistry}
 *
 * @author Stuart Douglas
 *
 */
public interface ServiceAwareObjectFactory extends ObjectFactory {

    void injectServiceRegistry(ServiceRegistry registry);

}
