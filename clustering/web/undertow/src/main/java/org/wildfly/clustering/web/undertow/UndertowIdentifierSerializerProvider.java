/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.session.IdentifierMarshaller;
import org.wildfly.clustering.session.IdentifierMarshallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentifierMarshallerProvider.class)
public class UndertowIdentifierSerializerProvider implements IdentifierMarshallerProvider {

    @Override
    public IdentifierMarshaller getMarshaller() {
        // Disable session ID marshalling optimization for custom alphabets
        String customAlphabet = System.getProperty("io.undertow.server.session.SecureRandomSessionIdGenerator.ALPHABET");
        return (customAlphabet == null) ? IdentifierMarshaller.BASE64 : IdentifierMarshaller.ISO_LATIN_1;
    }
}
