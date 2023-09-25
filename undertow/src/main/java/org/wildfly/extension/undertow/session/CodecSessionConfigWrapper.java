/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import org.jboss.as.web.session.SessionIdentifierCodec;

import io.undertow.server.session.SessionConfig;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionConfigWrapper;

/**
 * Adds session identifier encoding/decoding to a {@link SessionConfig}.
 * @author Paul Ferraro
 */
public class CodecSessionConfigWrapper implements SessionConfigWrapper {

    private final SessionIdentifierCodec codec;

    public CodecSessionConfigWrapper(SessionIdentifierCodec codec) {
        this.codec = codec;
    }

    @Override
    public SessionConfig wrap(SessionConfig config, Deployment deployment) {
        return new CodecSessionConfig(config, this.codec);
    }
}
