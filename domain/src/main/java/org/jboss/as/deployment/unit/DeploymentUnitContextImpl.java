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

package org.jboss.as.deployment.unit;

import org.jboss.as.deployment.AttachmentKey;
import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation for DeploymentUnitContext.
 *  
 * @author John E. Bailey
 */
public class DeploymentUnitContextImpl implements DeploymentUnitContext {
    private final String name;
    private final VirtualFile virtualFile;
    private final List<DeploymentItem> deploymentItems = new ArrayList<DeploymentItem>();
    private final Map<AttachmentKey, Object> attachments = new HashMap<AttachmentKey, Object>();

    public DeploymentUnitContextImpl(String name, VirtualFile virtualFile) {
        this.name = name;
        this.virtualFile = virtualFile;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    @Override
    public void addDeploymentItem(DeploymentItem item) {
        deploymentItems.add(item);
    }

    public List<DeploymentItem> getDeploymentItems() {
        return new ArrayList<DeploymentItem>(deploymentItems);        
    }

    @Override
    public <T> T getAttachment(final AttachmentKey<T> key) {
        return safeCast(key, attachments.get(key));
    }

    @Override
    public <T> T putAttachment(final AttachmentKey<T> key, final T value) {
        if(value == null)
            return removeAttachment(key);
        return safeCast(key, attachments.put(key, value));
    }

    @Override
    public <T> T removeAttachment(final AttachmentKey<T> key) {
        return safeCast(key, attachments.remove(key)); 
    }

    private <T> T safeCast(AttachmentKey<T> key, final Object value) {
        return value != null ? key.getValueClass().cast(value) : null;
    }
}
