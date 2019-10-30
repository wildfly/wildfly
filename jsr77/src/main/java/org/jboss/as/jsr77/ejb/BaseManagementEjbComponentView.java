/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.ejb;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.jsr77.logging.JSR77Logger;
import org.jboss.as.naming.ManagedReference;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
abstract class BaseManagementEjbComponentView implements ComponentView {

    private volatile Map<String, Map<String, Method>> methods;

    public ManagedReference createInstance() {
        throw JSR77Logger.ROOT_LOGGER.onlyRequiredInLocalView();
    }

    public ManagedReference createInstance(Map<Object, Object> contextData) {
        throw JSR77Logger.ROOT_LOGGER.onlyRequiredInLocalView();
    }


    @Override
    public Component getComponent() {
        throw JSR77Logger.ROOT_LOGGER.onlyRequiredInLocalView();
    }

    @Override
    public Class<?> getProxyClass() {
        return null;
    }

    @Override
    public Class<?> getViewClass() {
        return null;
    }

    @Override
    public Set<Method> getViewMethods() {
        return null;
    }

    @Override
    public Method getMethod(String name, String descriptor) {
        Map<String, Map<String, Method>> methods = getMethods();
        Map<String, Method> methodsForName = methods.get(name);
        if (methodsForName != null) {
            return methodsForName.get(descriptor);
        }
        return null;
    }

    @Override
    public boolean isAsynchronous(Method method) {
        return false;
    }

    private Map<String, Map<String, Method>> getMethods() {
        Map<String, Map<String, Method>> methods = this.methods;
        if (methods != null) {
            return methods;
        }

        synchronized (this) {
            methods = this.methods;
            if (methods == null) {
                methods = initMethods();
                this.methods = methods;
            }
        }
        return methods;
    }

    abstract Map<String, Map<String, Method>> initMethods();
}
