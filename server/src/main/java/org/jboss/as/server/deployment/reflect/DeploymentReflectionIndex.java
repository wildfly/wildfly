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

package org.jboss.as.server.deployment.reflect;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.server.ServerMessages;

/**
 * A reflection index for a deployment.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DeploymentReflectionIndex {
    private final Map<Class<?>, ClassReflectionIndex<?>> classes = new HashMap<Class<?>, ClassReflectionIndex<?>>();

    DeploymentReflectionIndex() {
    }

    private static final Permission DEPLOYMENT_REFLECTION_INDEX_PERMISSION = new RuntimePermission("createDeploymentReflectionIndex");

    /**
     * Construct a new instance.
     *
     * @return the new instance
     */
    public static DeploymentReflectionIndex create() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(DEPLOYMENT_REFLECTION_INDEX_PERMISSION);
        }
        return new DeploymentReflectionIndex();
    }

    /**
     * Get the (possibly cached) index for a given class.
     *
     * @param clazz the class
     * @return the index
     */
    @SuppressWarnings({"unchecked"})
    public synchronized <T> ClassReflectionIndex<T> getClassIndex(Class<T> clazz) {
        try {
            ClassReflectionIndex<T> index = (ClassReflectionIndex<T>) classes.get(clazz);
            if (index == null) {
                classes.put(clazz, index = new ClassReflectionIndex<T>(clazz, this));
            }
            return index;
        } catch (Throwable e) {
            throw ServerMessages.MESSAGES.errorGettingReflectiveInformation(clazz, clazz.getClassLoader(), e);
        }
    }
}
