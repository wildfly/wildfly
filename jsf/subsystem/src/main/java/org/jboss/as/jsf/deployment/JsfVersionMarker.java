/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * @author Stuart Douglas
 */
public class JsfVersionMarker {

    public static final String JSF_4_0 = "Mojarra-4.0";
    public static final String WAR_BUNDLES_JSF_IMPL = "WAR_BUNDLES_JSF_IMPL";
    public static final String NONE = "NONE";

    private JsfVersionMarker() {

    }

    private static AttachmentKey<String> VERSION_KEY = AttachmentKey.create(String.class);

    public static void setVersion(final DeploymentUnit deploymentUnit, final String value) {
        deploymentUnit.putAttachment(VERSION_KEY, value);
    }

    public static String getVersion(final DeploymentUnit deploymentUnit) {
        final String version = deploymentUnit.getAttachment(VERSION_KEY);
        return version == null ? JSF_4_0 : version;
    }

    public static boolean isJsfDisabled(final DeploymentUnit deploymentUnit) {
        final String version = deploymentUnit.getAttachment(VERSION_KEY);
        return NONE.equals(version);
    }

}
