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

import static org.jboss.as.platform.mbean.PlatformMBeanUtil.escapeMBeanName;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Resource impl for the {@link java.lang.management.MemoryManagerMXBean} parent resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MemoryManagerMXBeanResource extends AbstractPlatformMBeanResource {

    MemoryManagerMXBeanResource() {
        super(PlatformMBeanConstants.MEMORY_MANAGER_PATH);
    }

    @Override
    ResourceEntry getChildEntry(String name) {
        for (MemoryManagerMXBean mbean : ManagementFactory.getMemoryManagerMXBeans()) {
            if (name.equals(escapeMBeanName(mbean.getName()))) {
                return new LeafPlatformMBeanResource(PathElement.pathElement(ModelDescriptionConstants.NAME, name));
            }
        }
        return null;
    }

    @Override
    Set<String> getChildrenNames() {
        final Set<String> result = new HashSet<String>();
        for (MemoryManagerMXBean mbean : ManagementFactory.getMemoryManagerMXBeans()) {
            result.add(escapeMBeanName(mbean.getName()));
        }
        return result;
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(ModelDescriptionConstants.NAME);
    }
}
