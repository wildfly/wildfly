/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.msc.service.ServiceName;

public final class AttachmentKeys {

    /** Names of misc services that should start before WeldStartCompletionService. */
    public static final AttachmentKey<AttachmentList<ServiceName>> START_COMPLETION_DEPENDENCIES = AttachmentKey.createList(ServiceName.class);

}
