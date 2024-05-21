/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;

import org.jboss.as.ee.component.ProxyInvocationHandler;
import org.jboss.invocation.proxy.ProxyFactory;
import org.kohsuke.MetaInfServices;
import org.wildfly.security.ParametricPrivilegedAction;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.clustering.server.immutable.Immutability;

/**
 * Immutability test for EJB proxies, whose serializable placeholders are immutable.
 * @author Paul Ferraro
 */
@MetaInfServices(Immutability.class)
public class StatefulSessionBeanImmutability implements Immutability {
    private static final ParametricPrivilegedAction<InvocationHandler, Object> GET_INVOCATION_HANDLER = new ParametricPrivilegedAction<>() {
        @Override
        public InvocationHandler run(Object object) {
            // Since there is a low probability that this object is actually a SFSB proxy, avoid Class.getDeclaredField(...) which will typically throw/catch a NoSuchFieldException
            for (Field field : object.getClass().getDeclaredFields()) {
                if (field.getName().equals(ProxyFactory.INVOCATION_HANDLER_FIELD)) {
                    field.setAccessible(true);
                    try {
                        return (InvocationHandler) field.get(object);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            return null;
        }
    };

    @Override
    public boolean test(Object object) {
        InvocationHandler handler = WildFlySecurityManager.doUnchecked(object, GET_INVOCATION_HANDLER);
        return handler instanceof ProxyInvocationHandler;
    }
}
