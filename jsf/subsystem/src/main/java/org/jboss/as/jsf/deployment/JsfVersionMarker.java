/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.jsf.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * @author Stuart Douglas
 */
public class JsfVersionMarker {

    public static final String JSF_2_0 = "Mojarra-2.0";
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
        return version == null ? JSF_2_0 : version;
    }

    public static boolean isJsfDisabled(final DeploymentUnit deploymentUnit) {
        final String version = deploymentUnit.getAttachment(VERSION_KEY);
        return NONE.equals(version);
    }

}
