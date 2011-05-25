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

package org.jboss.as.ejb3.component.session;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.PrimitiveClassLoaderUtil;
import org.jboss.as.ejb3.component.EJBBusinessMethod;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.EJBMethodDescription;
import org.jboss.as.ejb3.deployment.EjbJarConfiguration;

import javax.ejb.AccessTimeout;
import javax.ejb.LockType;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * User: jpai
 */
public abstract class SessionBeanComponentCreateService extends EJBComponentCreateService {

    private final LockType beanLevelLockType;

    private final Map<EJBBusinessMethod, LockType> methodApplicableLockTypes;

    private final AccessTimeout beanLevelAccessTimeout;

    private final Map<EJBBusinessMethod, AccessTimeout> methodApplicableAccessTimeouts;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public SessionBeanComponentCreateService(final ComponentConfiguration componentConfiguration, final EjbJarConfiguration ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final SessionBeanComponentDescription sessionBeanComponentDescription = (SessionBeanComponentDescription) componentConfiguration.getComponentDescription();
        this.beanLevelLockType = sessionBeanComponentDescription.getBeanLevelLockType() == null ? LockType.WRITE : sessionBeanComponentDescription.getBeanLevelLockType();
        Map<EJBMethodDescription, LockType> methodLocks = sessionBeanComponentDescription.getMethodApplicableLockTypes();
        if (methodLocks == null) {
            this.methodApplicableLockTypes = Collections.emptyMap();
        } else {
            final Map<EJBBusinessMethod, LockType> locks = new HashMap();
            for (Map.Entry<EJBMethodDescription, LockType> entry : methodLocks.entrySet()) {
                final EJBMethodDescription ejbMethodDescription = entry.getKey();
                final EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(ejbMethodDescription);
                locks.put(ejbMethod, entry.getValue());
            }
            this.methodApplicableLockTypes = Collections.unmodifiableMap(locks);
        }

        AccessTimeout accessTimeout = sessionBeanComponentDescription.getBeanLevelAccessTimeout();
        // TODO: the configuration should always have an access timeout
        if (accessTimeout == null) {
            accessTimeout = new AccessTimeout() {
                @Override
                public long value() {
                    return 5;
                }

                @Override
                public TimeUnit unit() {
                    return MINUTES;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return AccessTimeout.class;
                }
            };
        }
        this.beanLevelAccessTimeout = accessTimeout;

        final Map<EJBMethodDescription, AccessTimeout> methodAccessTimeouts = sessionBeanComponentDescription.getMethodApplicableAccessTimeouts();
        if (methodAccessTimeouts == null) {
            this.methodApplicableAccessTimeouts = Collections.emptyMap();
        } else {
            final Map<EJBBusinessMethod, AccessTimeout> accessTimeouts = new HashMap();
            for (Map.Entry<EJBMethodDescription, AccessTimeout> entry : methodAccessTimeouts.entrySet()) {
                final EJBMethodDescription ejbMethodDescription = entry.getKey();
                final EJBBusinessMethod ejbMethod = this.getEJBBusinessMethod(ejbMethodDescription);
                accessTimeouts.put(ejbMethod, entry.getValue());
            }
            this.methodApplicableAccessTimeouts = Collections.unmodifiableMap(accessTimeouts);
        }

    }

    public LockType getBeanLockType() {
        return this.beanLevelLockType;
    }

    public Map<EJBBusinessMethod, LockType> getMethodApplicableLockTypes() {
        return this.methodApplicableLockTypes;
    }

    public Map<EJBBusinessMethod, AccessTimeout> getMethodApplicableAccessTimeouts() {
        return this.methodApplicableAccessTimeouts;
    }

    public AccessTimeout getBeanAccessTimeout() {
        return this.beanLevelAccessTimeout;
    }

    private EJBBusinessMethod getEJBBusinessMethod(final EJBMethodDescription method) {
        final ClassLoader classLoader = this.getComponentClass().getClassLoader();
        final String methodName = method.getMethodName();
        final String[] types = method.getMethodParams();
        if (types == null || types.length == 0) {
            return new EJBBusinessMethod(methodName);
        }
        Class<?>[] paramTypes = new Class<?>[types.length];
        int i = 0;
        for (String type : types) {
            try {
                paramTypes[i++] = PrimitiveClassLoaderUtil.loadClass(type, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return new EJBBusinessMethod(methodName, paramTypes);
    }


}
