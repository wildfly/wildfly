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

package org.wildfly.extension.undertow.session;

import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerConfig {

    public static final ServiceName SHARED_SESSION_MANAGER_SERVICE_NAME = ServiceName.of("undertow", "shared-session-manager");
    public static final ServiceName SHARED_SESSION_IDENTIFIER_CODEC_SERVICE_NAME = SHARED_SESSION_MANAGER_SERVICE_NAME.append("codec");

    private int maxActiveSessions = -1;
    private ReplicationConfig replicationConfig;
    private SessionConfigMetaData sessionConfig;

    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public void setMaxActiveSessions(int maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }

    public ReplicationConfig getReplicationConfig() {
        return replicationConfig;
    }

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
