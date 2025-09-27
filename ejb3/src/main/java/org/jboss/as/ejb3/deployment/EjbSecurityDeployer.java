/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ee.security.AbstractSecurityDeployer;
import org.jboss.as.ee.security.JaccService;
import org.jboss.as.ejb3.security.EjbJaccConfig;
import org.jboss.as.ejb3.security.EjbJaccService;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;

/**
 * Handles Jakarta Enterprise Beans jar deployments
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EjbSecurityDeployer extends AbstractSecurityDeployer<AttachmentList<EjbJaccConfig>> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected AttachmentKey<AttachmentList<EjbJaccConfig>> getMetaDataType() {
        return EjbDeploymentAttachmentKeys.JACC_PERMISSIONS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JaccService<AttachmentList<EjbJaccConfig>> createService(String contextId, AttachmentList<EjbJaccConfig> metaData, Boolean standalone, ClassLoader deploymentClassLoader) {
        return new EjbJaccService(contextId, metaData, standalone, deploymentClassLoader);
    }
}
