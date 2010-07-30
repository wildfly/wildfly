/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.module;

import org.jboss.as.deployment.AttachmentKey;

import java.io.Serializable;

/**
 * Mount configuration attachment to control mounting semantics. 
 *
 * @author John E. Bailey
 */
public class VFSMountConfig implements Serializable {
    private static final long serialVersionUID = 4090634336529594421L;
    public static final AttachmentKey<VFSMountConfig> ATTACHMENT_KEY = new AttachmentKey<VFSMountConfig>(VFSMountConfig.class);

    private final boolean expanded;

    public VFSMountConfig(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Should the vfs mount be expanded
     *
     * @return true if it should be expanded false if not.
     */
    public boolean isExpanded() {
        return expanded;
    }
}
