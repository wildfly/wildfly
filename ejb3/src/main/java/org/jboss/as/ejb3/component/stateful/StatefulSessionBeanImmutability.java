/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
