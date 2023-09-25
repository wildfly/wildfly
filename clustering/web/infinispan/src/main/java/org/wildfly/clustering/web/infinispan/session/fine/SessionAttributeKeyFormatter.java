/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.fine;

import java.util.UUID;
import java.util.function.Function;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.DelimitedFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;

/**
 * Formatter for a {@link SessionAttributeKey}
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class SessionAttributeKeyFormatter extends DelimitedFormatter<SessionAttributeKey> {

    public SessionAttributeKeyFormatter() {
        super(SessionAttributeKey.class, "#", new Function<String[], SessionAttributeKey>() {
            @Override
            public SessionAttributeKey apply(String[] parts) {
                return new SessionAttributeKey(parts[0], UUID.fromString(parts[1]));
            }
        }, new Function<SessionAttributeKey, String[]>() {
            @Override
            public String[] apply(SessionAttributeKey key) {
                return new String[] { key.getId(), key.getAttributeId().toString() };
            }
        });
    }
}