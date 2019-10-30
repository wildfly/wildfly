/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jsf.injection;

import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.WebInjectionContainer;

/**
 * {@link InjectionProvider} implementation which provides MyFaces 2.2 support
 *
 * @author Dmitrii Tikhomirov dtikhomi@redhat.com (C) 2016 Red Hat Inc.
 */
public class MyFacesInjectionProvider extends InjectionProvider {

    private final WebInjectionContainer injectionContainer;

    public MyFacesInjectionProvider() {
        this.injectionContainer = StartupContext.getInjectionContainer();
    }

    @Override
    public Object inject(Object instance) throws InjectionProviderException {
        return null;
    }

    @Override
    public void postConstruct(Object instance, Object creationMetaData) throws InjectionProviderException {
        if (injectionContainer != null) {
            try {
                injectionContainer.newInstance(instance);
            } catch (Exception e) {
                throw new InjectionProviderException(e);
            }
        }
    }

    @Override
    public void preDestroy(Object instance, Object creationMetaData) throws InjectionProviderException {
        if (injectionContainer != null) {
            injectionContainer.destroyInstance(instance);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
