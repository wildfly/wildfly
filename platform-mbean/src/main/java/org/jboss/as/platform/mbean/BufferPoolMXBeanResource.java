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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Resource impl for the {@code java.lang.management.BufferPoolMXBean}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BufferPoolMXBeanResource extends AbstractPlatformMBeanResource {

    BufferPoolMXBeanResource() {
        super(PlatformMBeanConstants.BUFFER_POOL_PATH);
    }

    @Override
    ResourceEntry getChildEntry(String name) {
        if (getChildrenNames().contains(name)) {
            return new LeafPlatformMBeanResource(PathElement.pathElement(ModelDescriptionConstants.NAME, name));
        }
        return null;
    }

    @Override
    Set<String> getChildrenNames() {
        try {
            final Set<String> result = new HashSet<String>();
            final ObjectName pattern = new ObjectName(PlatformMBeanConstants.BUFFER_POOL_MXBEAN_DOMAIN_TYPE + ",name=*");
            Set<ObjectName> names = ManagementFactory.getPlatformMBeanServer().queryNames(pattern, null);
            for (ObjectName on : names) {
                result.add(escapeMBeanName(on.getKeyProperty(ModelDescriptionConstants.NAME)));
            }
            return result;
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getChildTypes() {
        return Collections.singleton(ModelDescriptionConstants.NAME);
    }
}
