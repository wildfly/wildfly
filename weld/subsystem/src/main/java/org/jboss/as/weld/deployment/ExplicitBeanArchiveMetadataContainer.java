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

import java.util.Map;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.weld.util.collections.ImmutableMap;

/**
 * Information about explicit CDI bean archives
 * <p>
 * Thread Safety: This class is immutable and does not require a happens before event between construction and usage
 *
 * @author Stuart Douglas
 * @author Jozef Hartinger
 *
 */
public class ExplicitBeanArchiveMetadataContainer {

    public static final AttachmentKey<ExplicitBeanArchiveMetadataContainer> ATTACHMENT_KEY = AttachmentKey.create(ExplicitBeanArchiveMetadataContainer.class);

    private final Map<ResourceRoot, ExplicitBeanArchiveMetadata> beanArchiveMetadata;

    public ExplicitBeanArchiveMetadataContainer(Map<ResourceRoot, ExplicitBeanArchiveMetadata> beanArchiveMetadata) {
        this.beanArchiveMetadata = ImmutableMap.copyOf(beanArchiveMetadata);
    }

    public Map<ResourceRoot, ExplicitBeanArchiveMetadata> getBeanArchiveMetadata() {
        return beanArchiveMetadata;
    }

}
