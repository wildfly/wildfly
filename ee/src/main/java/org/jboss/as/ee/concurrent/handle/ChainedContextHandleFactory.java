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

import org.jboss.as.ee.EeLogger;

import javax.enterprise.concurrent.ContextService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A context handle factory which creates context handle's that internally are a chain of other context handles.
 *
 * @author Eduardo Martins
 */
public class ChainedContextHandleFactory implements ContextHandleFactory {

    private static final ContextHandleFactory[] EMPTY = {};

    private volatile ContextHandleFactory[] factories;
    private final Type type;

    /**
     * @param type
     */
    public ChainedContextHandleFactory(Type type) {
        this.factories = EMPTY;
        this.type = type;
    }

    public synchronized void add(ContextHandleFactory factoryToAdd) {
        final Comparator<ContextHandleFactory> comparator = new Comparator<ContextHandleFactory>() {
            @Override
            public int compare(ContextHandleFactory o1, ContextHandleFactory o2) {
                return o1.getType().compareTo(o2.getType());
            }
        };
        SortedSet<ContextHandleFactory> sortedSet = new TreeSet<>(comparator);
        for (ContextHandleFactory factory : factories) {
            sortedSet.add(factory);
        }
        sortedSet.add(factoryToAdd);
        factories = sortedSet.toArray(new ContextHandleFactory[sortedSet.size()]);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        final List<ContextHandle> handles = new ArrayList<>();
        for (ContextHandleFactory factory : factories) {
            handles.add(factory.saveContext(contextService, contextObjectProperties));
        }
        return new ChainedContextHandle(handles);
    }

    private static class ChainedContextHandle implements ContextHandle {

        private final List<ContextHandle> setupHandles;
        private LinkedList<ContextHandle> resetHandles;

        private ChainedContextHandle(List<ContextHandle> setupHandles) {
            this.setupHandles = setupHandles;
        }

        @Override
        public void setup() throws IllegalStateException {
            resetHandles = new LinkedList<>();
            try {
                for (ContextHandle handle : this.setupHandles) {
                    handle.setup();
                    resetHandles.addFirst(handle);
                }
            } catch (Error | RuntimeException e) {
                reset();
                throw e;
            }
        }

        @Override
        public void reset() {
            for (ContextHandle handle : resetHandles) {
                try {
                    handle.reset();
                } catch (Throwable e) {
                    EeLogger.ROOT_LOGGER.debug("failed to reset handle",e);
                }
            }
        }
    }

}
