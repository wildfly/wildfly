/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow;

import java.security.PrivilegedAction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.session.IdentifierMarshaller;
import org.wildfly.clustering.session.IdentifierMarshallerProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentifierMarshallerProvider.class)
public class UndertowIdentifierSerializerProvider implements IdentifierMarshallerProvider, PrivilegedAction<String> {

    @Override
    public IdentifierMarshaller getMarshaller() {
        // Disable session ID marshalling optimization for custom alphabets
        String customAlphabet = WildFlySecurityManager.doUnchecked(this);
        return (customAlphabet == null) ? IdentifierMarshaller.BASE64 : IdentifierMarshaller.ISO_LATIN_1;
    }

    @Override
    public String run() {
        return System.getProperty("io.undertow.server.session.SecureRandomSessionIdGenerator.ALPHABET");
    }
}
