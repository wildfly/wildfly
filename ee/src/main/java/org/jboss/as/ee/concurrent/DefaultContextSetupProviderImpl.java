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
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.jboss.as.ee.EeMessages;
import org.jboss.as.ee.concurrent.handle.ContextHandle;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The default context setup provider.  delegates context saving/setting/resetting to the context handle factory provided by the current concurrent context.
 *
 * @author Eduardo Martins
 */
public class DefaultContextSetupProviderImpl implements ContextSetupProvider {

    private ConcurrentContext getConcurrentContext() throws IllegalStateException {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext == null) {
            throw EeMessages.MESSAGES.noConcurrentContextCurrentlySet();
        }
        return concurrentContext;
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle saveContext(ContextService contextService) {
        return getConcurrentContext().getAllChainedContextHandleFactory().saveContext(contextService, null);
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return getConcurrentContext().getAllChainedContextHandleFactory().saveContext(contextService, contextObjectProperties);
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle setup(org.glassfish.enterprise.concurrent.spi.ContextHandle contextHandle) throws IllegalStateException {
        ((ContextHandle) contextHandle).setup();
        return contextHandle;
    }

    @Override
    public void reset(org.glassfish.enterprise.concurrent.spi.ContextHandle contextHandle) {
        ((ContextHandle) contextHandle).reset();
    }
}
