/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import org.wildfly.clustering.session.ImmutableSessionMetaData;

/**
 * Recordable {@link InactiveSessionStatistics}.
 * @author Paul Ferraro
 */
public interface RecordableInactiveSessionStatistics extends InactiveSessionStatistics, Recordable<ImmutableSessionMetaData> {

}
