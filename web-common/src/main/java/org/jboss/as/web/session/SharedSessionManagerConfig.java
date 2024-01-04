/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.session;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerConfig {

    public static final AttachmentKey<SharedSessionManagerConfig> ATTACHMENT_KEY = AttachmentKey.create(SharedSessionManagerConfig.class);
    public static final ServiceName SHARED_SESSION_MANAGER_SERVICE_NAME = ServiceName.of("web", "shared-session-manager");
    public static final ServiceName SHARED_SESSION_AFFINITY_SERVICE_NAME = SHARED_SESSION_MANAGER_SERVICE_NAME.append("affinity");

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

    @Deprecated
    public ReplicationConfig getReplicationConfig() {
        return replicationConfig;
    }

    @Deprecated
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
