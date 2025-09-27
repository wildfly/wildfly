/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.ear.spec.EarMetaData;

/**
 * Handles ear deployments
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EarSecurityDeployer extends AbstractSecurityDeployer<EarMetaData> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected JaccService<EarMetaData> createService(String contextId, EarMetaData metaData, Boolean standalone, ClassLoader deploymentClassLoader) {
        return new EarJaccService(contextId, metaData, standalone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AttachmentKey<EarMetaData> getMetaDataType() {
        return Attachments.EAR_METADATA;
    }

}
