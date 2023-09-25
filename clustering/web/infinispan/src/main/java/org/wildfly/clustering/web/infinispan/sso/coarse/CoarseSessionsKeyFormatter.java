/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso.coarse;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Formatter for {@link CoarseSessionsKey}.
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class CoarseSessionsKeyFormatter extends SessionKeyFormatter<CoarseSessionsKey> {

    public CoarseSessionsKeyFormatter() {
        super(CoarseSessionsKey.class, CoarseSessionsKey::new);
    }
}
