/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
 * A {@link Service} which can be used by other components like WS to get the security metadata associated with methods on a Jakarta Enterprise Beans view.
 *
 * @author Jaikiran Pai
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
