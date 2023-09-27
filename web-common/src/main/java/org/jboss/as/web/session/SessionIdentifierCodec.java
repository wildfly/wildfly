/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.session;

/**
 * Encapsulates logic to encode/decode additional information in/from a session identifier.
 * Both the {@link #encode(CharSequence)} and {@link #decode(CharSequence)} methods should be idempotent.
 * The codec methods should also be symmetrical.  i.e. the result of
 * <code>decode(encode(x))</code> should yield <code>x</code>, just as the result of
 * <code>encode(decode(y))</code> should yield <code>y</code>.
 * @author Paul Ferraro
 */
public interface SessionIdentifierCodec {
    /**
     * Encodes the specified session identifier
     * @param sessionId a session identifier
     * @return an encoded session identifier
     */
    CharSequence encode(CharSequence sessionId);

    /**
     * Decodes the specified session identifier encoded via {@link #encode(CharSequence)}.
     * @param encodedSessionId an encoded session identifier
     * @return the decoded session identifier
     */
    CharSequence decode(CharSequence encodedSessionId);
}
