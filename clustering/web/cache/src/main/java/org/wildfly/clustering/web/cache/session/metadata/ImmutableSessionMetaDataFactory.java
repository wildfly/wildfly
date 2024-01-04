/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public interface ImmutableSessionMetaDataFactory<V> extends Locator<String, V> {
    ImmutableSessionMetaData createImmutableSessionMetaData(String id, V value);
}
