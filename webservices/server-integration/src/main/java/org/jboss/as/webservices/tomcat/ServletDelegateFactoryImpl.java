/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
