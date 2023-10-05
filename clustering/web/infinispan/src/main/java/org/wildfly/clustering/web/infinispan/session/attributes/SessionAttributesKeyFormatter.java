/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session.attributes;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Resolver for {@link SessionAttributesKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class SessionAttributesKeyFormatter extends SessionKeyFormatter<SessionAttributesKey> {

    public SessionAttributesKeyFormatter() {
        super(SessionAttributesKey.class, SessionAttributesKey::new);
    }
}
