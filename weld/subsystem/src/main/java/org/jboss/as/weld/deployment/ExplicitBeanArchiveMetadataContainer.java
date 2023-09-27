/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
