/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan;

import java.util.List;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;
import org.wildfly.clustering.web.cache.session.attributes.fine.FineSessionAttributesSerializationContextInitializer;
import org.wildfly.clustering.web.cache.session.metadata.coarse.CoarseSessionMetaDataSerializationContextInitializer;
import org.wildfly.clustering.web.infinispan.session.attributes.SessionAttributesSerializationContextInitializer;
import org.wildfly.clustering.web.infinispan.session.metadata.SessionMetaDataSerializationContextInitializer;
import org.wildfly.clustering.web.infinispan.sso.SSOSerializationContextInitializer;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class InfinispanWebSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public InfinispanWebSerializationContextInitializer() {
        // Initialize only those marshallers used by this implementation
        super(List.of(
                new CoarseSessionMetaDataSerializationContextInitializer(),
                new FineSessionAttributesSerializationContextInitializer(),
                new SessionMetaDataSerializationContextInitializer(),
                new SessionAttributesSerializationContextInitializer(),
                new org.wildfly.clustering.web.cache.sso.SSOSerializationContextInitializer(),
                new SSOSerializationContextInitializer(),
                new org.wildfly.clustering.web.cache.sso.coarse.CoarseSSOSerializationContextInitializer(),
                new CoarseSSOSerializationContextInitializer()));
    }
}
