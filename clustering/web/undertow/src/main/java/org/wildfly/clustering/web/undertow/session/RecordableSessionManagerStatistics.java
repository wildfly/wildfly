/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.session.ImmutableSessionMetaData;

import io.undertow.server.session.SessionManagerStatistics;

/**
 * Recordable {@link SessionManagerStatistics}.
 * @author Paul Ferraro
 */
public interface RecordableSessionManagerStatistics extends SessionManagerStatistics, Recordable<ImmutableSessionMetaData> {
    Recordable<ImmutableSessionMetaData> getInactiveSessionRecorder();
}
