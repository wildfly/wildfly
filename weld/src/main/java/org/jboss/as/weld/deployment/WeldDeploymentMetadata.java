/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Information about a Weld deployment that is attached to the deployment
 * <p>
 * Thread Safety: This class is immutable and does not require a happens before event between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class WeldDeploymentMetadata {

    public static final AttachmentKey<WeldDeploymentMetadata> ATTACHMENT_KEY = AttachmentKey.create(WeldDeploymentMetadata.class);

    private final Set<BeanArchiveMetadata> beanArchiveMetadata;

    public WeldDeploymentMetadata(Set<BeanArchiveMetadata> beanArchiveMetadata) {
        this.beanArchiveMetadata = new HashSet<BeanArchiveMetadata>(beanArchiveMetadata);
    }

    public Set<BeanArchiveMetadata> getBeanArchiveMetadata() {
        return Collections.unmodifiableSet(beanArchiveMetadata);
    }

}
