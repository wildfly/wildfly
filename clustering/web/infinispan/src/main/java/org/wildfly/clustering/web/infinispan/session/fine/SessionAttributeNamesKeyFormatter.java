/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session.fine;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Formatter for {@link SessionAttributeNamesKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class SessionAttributeNamesKeyFormatter extends SessionKeyFormatter<SessionAttributeNamesKey> {

    public SessionAttributeNamesKeyFormatter() {
        super(SessionAttributeNamesKey.class, SessionAttributeNamesKey::new);
    }
}
