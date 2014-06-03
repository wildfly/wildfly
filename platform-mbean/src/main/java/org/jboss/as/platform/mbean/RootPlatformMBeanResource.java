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

package org.jboss.as.platform.mbean;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Resource for the root platform mbean resource tree.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RootPlatformMBeanResource extends AbstractPlatformMBeanResource {

    public RootPlatformMBeanResource() {
        super(PlatformMBeanConstants.ROOT_PATH);
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(ModelDescriptionConstants.TYPE);
    }

    @Override
    Set<String> getChildrenNames() {
        return new HashSet<String>(PlatformMBeanConstants.BASE_TYPES);
    }

    ResourceEntry getChildEntry(String name) {

        if (PlatformMBeanConstants.CLASS_LOADING.equals(name)) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.CLASS_LOADING_PATH);
        } else if (PlatformMBeanConstants.COMPILATION.equals(name) && ManagementFactory.getCompilationMXBean() != null) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.COMPILATION_PATH);
        } else if (PlatformMBeanConstants.GARBAGE_COLLECTOR.equals(name)) {
            return new GarbageCollectorMXBeanResource();
        } else if (PlatformMBeanConstants.MEMORY.equals(name)) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.MEMORY_PATH);
        } else if (PlatformMBeanConstants.MEMORY_MANAGER.equals(name)) {
            return new MemoryManagerMXBeanResource();
        } else if (PlatformMBeanConstants.MEMORY_POOL.equals(name)) {
            return new MemoryPoolMXBeanResource();
        } else if (PlatformMBeanConstants.OPERATING_SYSTEM.equals(name)) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.OPERATING_SYSTEM_PATH);
        } else if (PlatformMBeanConstants.RUNTIME.equals(name)) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.RUNTIME_PATH);
        } else if (PlatformMBeanConstants.THREADING.equals(name)) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.THREADING_PATH);
        } else if (PlatformMBeanConstants.BUFFER_POOL.equals(name)) {
            return new BufferPoolMXBeanResource();
        } else if (PlatformMBeanConstants.LOGGING.equals(name)) {
            return new LeafPlatformMBeanResource(PlatformMBeanConstants.LOGGING_PATH);
        } else {
            return null;
        }
    }
}
