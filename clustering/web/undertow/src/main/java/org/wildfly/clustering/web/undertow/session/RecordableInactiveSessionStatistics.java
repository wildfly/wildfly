/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.ee.Recordable;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.InactiveSessionStatistics;

/**
 * Recordable {@link InactiveSessionStatistics}.
 * @author Paul Ferraro
 */
public interface RecordableInactiveSessionStatistics extends InactiveSessionStatistics, Recordable<ImmutableSessionMetaData> {

}
