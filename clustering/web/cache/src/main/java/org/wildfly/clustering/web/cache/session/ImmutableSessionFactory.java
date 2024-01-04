/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.util.Map;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.web.cache.session.attributes.ImmutableSessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Factory for creating an {@link ImmutableSession}.
 * @author Paul Ferraro
 */
public interface ImmutableSessionFactory<MV, AV> extends Locator<String, Map.Entry<MV, AV>>{
    ImmutableSessionMetaDataFactory<MV> getMetaDataFactory();
    ImmutableSessionAttributesFactory<AV> getAttributesFactory();

    default ImmutableSession createImmutableSession(String id, Map.Entry<MV, AV> entry) {
        ImmutableSessionMetaData metaData = this.getMetaDataFactory().createImmutableSessionMetaData(id, entry.getKey());
        ImmutableSessionAttributes attributes = this.getAttributesFactory().createImmutableSessionAttributes(id, entry.getValue());
        return this.createImmutableSession(id, metaData, attributes);
    }

    ImmutableSession createImmutableSession(String id, ImmutableSessionMetaData metaData, ImmutableSessionAttributes attributes);
}
