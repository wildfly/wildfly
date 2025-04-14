/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.session;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.spec.SessionConfigMetaData;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerConfig {

    public static final AttachmentKey<SharedSessionManagerConfig> ATTACHMENT_KEY = AttachmentKey.create(SharedSessionManagerConfig.class);

    private boolean distributable = false;
    private Integer maxActiveSessions;
    private ReplicationConfig replicationConfig;
    private SessionConfigMetaData sessionConfig;

    public boolean isDistributable() {
        return this.distributable;
    }

    public void setDistributable(boolean distributable) {
        this.distributable = distributable;
    }

    public Integer getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public void setMaxActiveSessions(Integer maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }

    @Deprecated(forRemoval = true)
    public ReplicationConfig getReplicationConfig() {
        return replicationConfig;
    }

    @Deprecated(forRemoval = true)
    public void setReplicationConfig(ReplicationConfig replicationConfig) {
        this.replicationConfig = replicationConfig;
    }

    public SessionConfigMetaData getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfigMetaData sessionConfig) {
        this.sessionConfig = sessionConfig;
    }
}
