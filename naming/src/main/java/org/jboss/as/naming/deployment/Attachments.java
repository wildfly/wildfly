/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.deployment;

import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.server.deployment.AttachmentKey;

/**
 * The Naming subsystem's DU attachment keys.
 * @author Eduardo Martins
 */
public final class Attachments {

    /**
     * The DU key where the subsystem's {@link ExternalContexts} instance may be found by DUPs.
     */
    public static final AttachmentKey<ExternalContexts> EXTERNAL_CONTEXTS = AttachmentKey.create(ExternalContexts.class);
}
