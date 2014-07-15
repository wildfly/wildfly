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
import org.jboss.as.ee.concurrent.handle.ResetContextHandle;
import org.jboss.as.ee.concurrent.handle.SetupContextHandle;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.concurrent.handle.NullContextHandle;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * The default context setup provider.  delegates context saving/setting/resetting to the context handle factory provided by the current concurrent context.
 *
 * @author Eduardo Martins
 */
public class DefaultContextSetupProviderImpl implements ContextSetupProvider {

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle saveContext(ContextService contextService) {
        return saveContext(contextService, null);
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        final ConcurrentContext concurrentContext = ConcurrentContext.current();
        if (concurrentContext != null) {
            return concurrentContext.saveContext(contextService, contextObjectProperties);
        } else {
            EeLogger.ROOT_LOGGER.debug("ee concurrency context not found in invocation context");
            return NullContextHandle.INSTANCE;
        }
    }

    @Override
    public org.glassfish.enterprise.concurrent.spi.ContextHandle setup(org.glassfish.enterprise.concurrent.spi.ContextHandle contextHandle) throws IllegalStateException {
        return ((SetupContextHandle) contextHandle).setup();
    }

    @Override
    public void reset(org.glassfish.enterprise.concurrent.spi.ContextHandle contextHandle) {
        ((ResetContextHandle) contextHandle).reset();
    }
}
