/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.naming;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.ServiceName;

/**
 * EE related attachments.
 *
 * @author John Bailey
 */
public class Attachments {
    public static final AttachmentKey<ServiceName> APPLICATION_CONTEXT_CONFIG = AttachmentKey.create(ServiceName.class);
    public static final AttachmentKey<ServiceName> MODULE_CONTEXT_CONFIG = AttachmentKey.create(ServiceName.class);

    public static final AttachmentKey<JavaNamespaceSetup> JAVA_NAMESPACE_SETUP_ACTION = AttachmentKey.create(JavaNamespaceSetup.class);

}
