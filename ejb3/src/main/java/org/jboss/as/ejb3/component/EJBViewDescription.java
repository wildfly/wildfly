/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.msc.service.ServiceName;

/**
 * EJB specific view description.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EJBViewDescription extends ViewDescription {

    private final MethodIntf methodIntf;
    private final boolean hasJNDIBindings;

    public EJBViewDescription(final ComponentDescription componentDescription, final String viewClassName, final MethodIntf methodIntf) {
        super(componentDescription, viewClassName);
        this.methodIntf = methodIntf;
        hasJNDIBindings = initHasJNDIBindings(methodIntf);
    }

    public MethodIntf getMethodIntf() {
        return methodIntf;
    }

    @Override
    public ViewConfiguration createViewConfiguration(final Class<?> viewClass, final ComponentConfiguration componentConfiguration, final ProxyFactory<?> proxyFactory) {
        return new EJBViewConfiguration(viewClass, componentConfiguration, getServiceName(), proxyFactory, getMethodIntf());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EJBViewDescription that = (EJBViewDescription) o;

        // since the views are added to the component description, that should already be equal
        if (hasJNDIBindings != that.hasJNDIBindings) return false;
        if (methodIntf != that.methodIntf) return false;
        if (!getViewClassName().equals(that.getViewClassName())) return false;

        return true;
    }

    @Override // TODO: what to do in JNDI if multiple views are available for no interface view ?
    public ServiceName getServiceName() {
        return super.getServiceName().append(methodIntf.toString());
    }

    @Override
    public int hashCode() {
        int result = methodIntf.hashCode();
        result = 31 * result + (hasJNDIBindings ? 1 : 0);
        result = 31 * result + getViewClassName().hashCode();
        return result;
    }

    public boolean hasJNDIBindings() {
        return hasJNDIBindings;
    }

    private boolean initHasJNDIBindings(final MethodIntf methodIntf) {
        if (methodIntf == MethodIntf.MESSAGE_ENDPOINT) {
            return false;
        }
        if (methodIntf == MethodIntf.SERVICE_ENDPOINT) {
            return false;
        }
        if (methodIntf == MethodIntf.TIMER) {
            return false;
        }

        return true;
    }

}
