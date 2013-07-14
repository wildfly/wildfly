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

package org.jboss.as.ee.concurrent.naming;

import org.jboss.as.ee.EeMessages;
import org.jboss.as.ee.concurrent.ConcurrentContext;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The jndi default ContextService is a singleton bound to jndi, which just locates and delegates invocations to the real default ContextService, obtained from the concurrent context set when invoked.
 *
 * @author Eduardo Martins
 */
public class JndiDefaultContextService implements ContextService {

    private static JndiDefaultContextService ourInstance = new JndiDefaultContextService();

    public static JndiDefaultContextService getInstance() {
        return ourInstance;
    }

    private JndiDefaultContextService() {
    }

    private ContextService getCurrentContextService() throws IllegalStateException {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext == null) {
            throw EeMessages.MESSAGES.noConcurrentContextCurrentlySet();
        }
        return concurrentContext.getDefaultContextService();
    }

    @Override
    public Object createContextualProxy(Object instance, Class<?>... interfaces) throws IllegalArgumentException {
        return getCurrentContextService().createContextualProxy(instance, interfaces);
    }

    @Override
    public Object createContextualProxy(Object instance, Map<String, String> executionProperties, Class<?>... interfaces) throws IllegalArgumentException {
        return getCurrentContextService().createContextualProxy(instance, executionProperties, interfaces);
    }

    @Override
    public <T> T createContextualProxy(T instance, Class<T> _interface) throws IllegalArgumentException {
        return getCurrentContextService().createContextualProxy(instance, _interface);
    }

    @Override
    public <T> T createContextualProxy(T instance, Map<String, String> executionProperties, Class<T> _interface) throws IllegalArgumentException {
        return getCurrentContextService().createContextualProxy(instance, executionProperties, _interface);
    }

    @Override
    public Map<String, String> getExecutionProperties(Object contextualProxy) throws IllegalArgumentException {
        return getCurrentContextService().getExecutionProperties(contextualProxy);
    }


}
