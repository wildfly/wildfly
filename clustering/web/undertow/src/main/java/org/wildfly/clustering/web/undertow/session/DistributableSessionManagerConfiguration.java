/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Map;

import org.wildfly.clustering.session.SessionManager;

import io.undertow.server.session.SessionListeners;

/**
 * @author Paul Ferraro
 */
public interface DistributableSessionManagerConfiguration {
    String getDeploymentName();
    SessionManager<Map<String, Object>> getSessionManager();
    SessionListeners getSessionListeners();
    RecordableSessionManagerStatistics getStatistics();
}
