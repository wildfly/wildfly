/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
