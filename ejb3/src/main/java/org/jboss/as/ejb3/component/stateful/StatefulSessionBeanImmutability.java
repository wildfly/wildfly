/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.lang.reflect.InvocationHandler;

import org.jboss.as.ee.component.ProxyInvocationHandler;
import org.jboss.invocation.proxy.ProxyFactory;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.Immutability;

/**
 * Immutability test for EJB proxies, whose serializable placeholders are immutable.
 * @author Paul Ferraro
 */
@MetaInfServices(Immutability.class)
public class StatefulSessionBeanImmutability implements Immutability {

    @Override
    public boolean test(Object object) {
        try {
            InvocationHandler handler = ProxyFactory.getInvocationHandlerStatic(object);
            return handler instanceof ProxyInvocationHandler;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
