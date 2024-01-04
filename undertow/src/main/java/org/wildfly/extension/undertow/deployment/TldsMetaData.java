/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.SharedTldsMetaDataBuilder;
import org.jboss.metadata.web.spec.TldMetaData;

/**
 * @author Remy Maucherat
 */
public class TldsMetaData {

    public static final AttachmentKey<TldsMetaData> ATTACHMENT_KEY = AttachmentKey.create(TldsMetaData.class);

    /**
     * Shared TLDs.
     */
    private SharedTldsMetaDataBuilder sharedTlds;

    /**
     * Webapp TLDs.
     */
    private Map<String, TldMetaData> tlds;

    public List<TldMetaData> getSharedTlds(DeploymentUnit deploymentUnit) {
        return sharedTlds.getSharedTlds(deploymentUnit);
    }

    public void setSharedTlds(SharedTldsMetaDataBuilder sharedTlds) {
        this.sharedTlds = sharedTlds;
    }

    public Map<String, TldMetaData> getTlds() {
        return tlds;
    }

    public void setTlds(Map<String, TldMetaData> tlds) {
        this.tlds = tlds;
    }

}
