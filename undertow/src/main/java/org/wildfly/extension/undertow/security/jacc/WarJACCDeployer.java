/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.security.jacc;

import org.jboss.as.ee.security.AbstractSecurityDeployer;
import org.jboss.as.ee.security.JaccService;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.web.common.WarMetaData;

/**
 * Handles war deployments
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class WarJACCDeployer extends AbstractSecurityDeployer<WarMetaData> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected AttachmentKey<WarMetaData> getMetaDataType() {
        return WarMetaData.ATTACHMENT_KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JaccService<WarMetaData> createService(String contextId, WarMetaData metaData, Boolean standalone, ClassLoader deploymentClassLoader) {
        return new WarJACCService(contextId, metaData, standalone, deploymentClassLoader);
    }

}
