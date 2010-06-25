/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment;

import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.vfs.VirtualFile;

/**
 * The deployment unit context.  Instances of this interface are passed to each domain deployment on the server in order to serve
 * two purposes:
 * <ol>
 * <li>Allow for the addition of new deployment items which will be installed as a part of this deployment unit.</li>
 * <li>Provide a coordination point and type-safe storage location for the current processing state of the deployment.</li>
 * </ol>
 * Once deployment is complete, the deployment unit context need not be retained.
 */
public interface DeploymentUnitContext {

    /**
     * Get the simple name of the deployment unit.
     *
     * @return the simple name
     */
    String getName();

    /**
     * Get the root virtual file of the deployment unit.
     *
     * @return the root virtual file
     */
    VirtualFile getVirtualFile();

    /**
     * Add a deployment item to this deployment unit context.
     *
     * @param item
     */
    void addDeploymentItem(DeploymentItem item);

    /**
     * Get an attachment value.  If no attachment exists for this key, {@code null} is returned.
     *
     * @param key
     * @param <T>
     * @return
     */
    <T> T getAttachment(Key<T> key);

    /**
     * Set an attachment value.  If an attachment for this key was already set, return the original value.  If the value
     * being set is {@code null}, the attachment key is removed.
     *
     * @param key the attachment key
     * @param value the new value
     * @param <T> the value type
     * @return the old value, or {@code null} if there was none
     */
    <T> T putAttachment(Key<T> key, T value);

    <T> T removeAttachment(Key<T> key);

    interface Key<T> {
        Class<T> getValueClass();
    }
}
