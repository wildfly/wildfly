/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import io.undertow.server.session.SessionManagerStatistics;

import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Recordable {@link SessionManagerStatistics}.
 * @author Paul Ferraro
 */
public interface RecordableSessionManagerStatistics extends SessionManagerStatistics, Recordable<ImmutableSessionMetaData> {
    Recordable<ImmutableSessionMetaData> getInactiveSessionRecorder();
}
