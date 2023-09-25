/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Attachment keys for the JBeret batch subsystem.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface BatchAttachments {
    /**
     * The attachment key for the {@link BatchEnvironmentMetaData} used for deployment descriptors.
     */
    AttachmentKey<BatchEnvironmentMetaData> BATCH_ENVIRONMENT_META_DATA = AttachmentKey.create(BatchEnvironmentMetaData.class);

    /**
     * The attachment for the job operator.
     */
    AttachmentKey<WildFlyJobOperator> JOB_OPERATOR = AttachmentKey.create(WildFlyJobOperator.class);

    /**
     * The attachment key for the {@linkplain WildFlyJobXmlResolver job XML resolver}. Installed during the batch
     * environment processing.
     *
     * @see BatchEnvironmentProcessor
     */
    AttachmentKey<WildFlyJobXmlResolver> JOB_XML_RESOLVER = AttachmentKey.create(WildFlyJobXmlResolver.class);
}
