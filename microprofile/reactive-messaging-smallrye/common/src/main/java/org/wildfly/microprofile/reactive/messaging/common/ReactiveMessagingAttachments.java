/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.common;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingAttachments {
    public static AttachmentKey<Boolean> IS_REACTIVE_MESSAGING_DEPLOYMENT = AttachmentKey.create(Boolean.class);
}
