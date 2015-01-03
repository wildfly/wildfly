/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security.service;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.as.ejb3.security.EJBMethodSecurityAttribute;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A {@link Service} which can be used by other components like WS to get the security metadata associated with methods on an EJB view.
 *
 * @author: Jaikiran Pai
 * @see https://issues.jboss.org/browse/WFLY-308 for more details.
 */
public class EJBViewMethodSecurityAttributesService implements Service<EJBViewMethodSecurityAttributesService> {

    private static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("view-method-security-attributes");

    private final Map<Method, EJBMethodSecurityAttribute> methodSecurityAttributes;

    public EJBViewMethodSecurityAttributesService(final Map<Method, EJBMethodSecurityAttribute> securityAttributes) {
        this.methodSecurityAttributes = Collections.unmodifiableMap(new HashMap<>(securityAttributes));
    }

    @Override
    public void start(StartContext startContext) throws StartException {
    }

    @Override
    public void stop(StopContext stopContext) {
    }

    @Override
    public EJBViewMethodSecurityAttributesService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Returns the {@link EJBMethodSecurityAttribute} associated with the passed view method. This method returns null if no security attribute is applicable for the passed method
     *
     * @param viewMethod
     * @return
     */
    public EJBMethodSecurityAttribute getSecurityAttributes(final Method viewMethod) {
        return methodSecurityAttributes.get(viewMethod);
    }

    /**
     * Returns a {@link ServiceName} for the {@link EJBViewMethodSecurityAttributesService}
     *
     * @param appName       The application name to which the bean belongs. Can be null if the bean is <b>not</b> deployed in a .ear
     * @param moduleName    The module name to which the bean belongs
     * @param beanName      The bean name
     * @param viewClassName The fully qualified class name of the EJB view
     * @return
     */
    public static ServiceName getServiceName(final String appName, final String moduleName, final String beanName, final String viewClassName) {
        final ServiceName serviceName;
        if (appName != null) {
            serviceName = BASE_SERVICE_NAME.append(appName);
        } else {
            serviceName = BASE_SERVICE_NAME;
        }
        return serviceName.append(moduleName).append(beanName).append(viewClassName);
    }

    public static class Builder {
        private final Map<Method, EJBMethodSecurityAttribute> methodSecurityAttributes = new IdentityHashMap<>();

        public void addMethodSecurityMetadata(final Method viewMethod, final EJBMethodSecurityAttribute securityAttribute) {
            methodSecurityAttributes.put(viewMethod, securityAttribute);
        }

        public EJBViewMethodSecurityAttributesService build() {
            return new EJBViewMethodSecurityAttributesService(methodSecurityAttributes);
        }
    }
}
