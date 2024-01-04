/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.metadata;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Formatter for a {@link SessionMetaDataKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class SessionMetaDataKeyFormatter extends SessionKeyFormatter<SessionMetaDataKey> {

    public SessionMetaDataKeyFormatter() {
        super(SessionMetaDataKey.class, SessionMetaDataKey::new);
    }
}
