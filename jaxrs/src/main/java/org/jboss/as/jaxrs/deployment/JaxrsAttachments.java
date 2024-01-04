/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.modules.ModuleIdentifier;

import java.util.Map;

/**
 * Jaxrs attachments
 *
 * @author Stuart Douglas
 *
 */
public class JaxrsAttachments {

    public static final AttachmentKey<ResteasyDeploymentData> RESTEASY_DEPLOYMENT_DATA = AttachmentKey.create(ResteasyDeploymentData.class);
    public static final AttachmentKey<Map<ModuleIdentifier, ResteasyDeploymentData>> ADDITIONAL_RESTEASY_DEPLOYMENT_DATA = AttachmentKey.create(Map.class);

}
