/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.tomcat;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.ServletDelegate;
import org.jboss.wsf.spi.deployment.ServletDelegateFactory;

/**
 * WildFly implementation of {@link org.jboss.wsf.spi.deployment.ServletDelegateFactory}
 * that uses modular classloading for creating the delegate instance.
 *
 * @author alessio.soldano@jboss.com
 * @since 06-Apr-2011
 *
 */
public final class ServletDelegateFactoryImpl implements ServletDelegateFactory {

    @Override
    public ServletDelegate newServletDelegate(final String servletClassName) {
        final ClassLoaderProvider provider = ClassLoaderProvider.getDefaultProvider();
        try {
            final Class<?> clazz = provider.getServerIntegrationClassLoader().loadClass(servletClassName);
            return (ServletDelegate) clazz.newInstance();
        } catch (final Exception e) {
            throw WSLogger.ROOT_LOGGER.cannotInstantiateServletDelegate(e, servletClassName);
        }
    }

}
