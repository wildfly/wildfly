/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.ioc;

import org.jboss.as.webservices.WSServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.ioc.IoCContainerProxy;

/**
 * @see org.jboss.wsf.spi.ioc.IoCContainerProxy
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class IoCContainerProxyImpl implements IoCContainerProxy {
    /** Singleton. */
    private static final IoCContainerProxy SINGLETON = new IoCContainerProxyImpl();

    /**
     * Constructor.
     */
    public IoCContainerProxyImpl() {
        super();
    }

    /**
     * Returns container proxy instance.
     *
     * @return container proxy instance
     */
    static IoCContainerProxy getInstance() {
        return IoCContainerProxyImpl.SINGLETON;
    }

    /**
     * @see org.jboss.wsf.spi.ioc.IoCContainerProxy#getBean(java.lang.String, java.lang.Class)
     *
     * @param <T> bean type
     * @param beanName bean name inside IoC registry
     * @param clazz bean type class
     * @return bean instance
     * @throws IllegalArgumentException if bean is not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(final String beanName, final Class<T> clazz) {
        ServiceController<T> service = (ServiceController<T>)WSServices.getContainerRegistry().getService(ServiceName.parse(beanName));
        return service != null ? service.getValue() : null;
    }
}
