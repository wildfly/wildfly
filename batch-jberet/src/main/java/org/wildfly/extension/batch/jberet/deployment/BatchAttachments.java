/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
