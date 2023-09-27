/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.sso;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.web.infinispan.SessionKeyFormatter;

/**
 * Formatter for {@link AuthenticationKey}
 * @author Paul Ferraro
 */
@MetaInfServices(Formatter.class)
public class AuthenticationKeyFormatter extends SessionKeyFormatter<AuthenticationKey> {

    public AuthenticationKeyFormatter() {
        super(AuthenticationKey.class, AuthenticationKey::new);
    }
}
