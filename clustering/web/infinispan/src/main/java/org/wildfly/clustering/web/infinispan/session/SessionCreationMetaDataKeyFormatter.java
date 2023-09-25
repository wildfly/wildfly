/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Formatter for a {@link SessionCreationMetaDataKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class SessionCreationMetaDataKeyFormatter extends SessionKeyFormatter<SessionCreationMetaDataKey> {

    public SessionCreationMetaDataKeyFormatter() {
        super(SessionCreationMetaDataKey.class, SessionCreationMetaDataKey::new);
    }
}
