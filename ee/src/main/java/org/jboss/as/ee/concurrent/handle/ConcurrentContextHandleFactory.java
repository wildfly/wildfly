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
package org.jboss.as.ee.concurrent.handle;

import org.jboss.as.ee.concurrent.ConcurrentContext;

import javax.enterprise.concurrent.ContextService;
import java.util.Map;

/**
 * A context handle factory responsible for saving and setting proper java ee concurrent context
 *
 * @author Eduardo Martins
 */
public class ConcurrentContextHandleFactory implements ContextHandleFactory {

    public static final ConcurrentContextHandleFactory INSTANCE = new ConcurrentContextHandleFactory();

    private ConcurrentContextHandleFactory() {
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new ConcurrentContextHandle();
    }

    @Override
    public Type getType() {
        return Type.CONCURRENT;
    }

    private static class ConcurrentContextHandle implements ContextHandle {

        private final ConcurrentContext concurrentContext;

        private ConcurrentContextHandle() {
            this.concurrentContext = ConcurrentContext.current();
        }

        @Override
        public void setup() throws IllegalStateException {
            if (concurrentContext != null) {
                ConcurrentContext.pushCurrent(concurrentContext);
            }
        }

        @Override
        public void reset() {
            if (concurrentContext != null) {
                ConcurrentContext.popCurrent();
            }
        }
    }
}
