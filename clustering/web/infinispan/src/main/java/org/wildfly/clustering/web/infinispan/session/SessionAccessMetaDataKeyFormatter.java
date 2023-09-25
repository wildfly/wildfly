/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Formatter for a {@link SessionAccessMetaDataKey}
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class SessionAccessMetaDataKeyFormatter extends SessionKeyFormatter<SessionAccessMetaDataKey> {

    public SessionAccessMetaDataKeyFormatter() {
        super(SessionAccessMetaDataKey.class, SessionAccessMetaDataKey::new);
    }
}
