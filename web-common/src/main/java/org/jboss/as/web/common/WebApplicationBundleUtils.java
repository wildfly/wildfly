/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.web.common;

import java.util.Locale;
import java.util.jar.Manifest;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.ManifestHelper;

/**
 * Utility methods for OSGi WebApplications (WAB).
 *
 * @author Thomas.Diesler@jboss.com
 * @since 15-MAr-2013
 */
public class WebApplicationBundleUtils {

    public static boolean isWebApplicationBundle(DeploymentUnit depUnit) {

        // JAR deployments may contain OSGi metadata with a "Web-ContextPath" header
        // This qualifies them as OSGi Web Application Bundle (WAB)
        String deploymentName = depUnit.getName().toLowerCase(Locale.ENGLISH);
        Manifest manifest = depUnit.getAttachment(Attachments.OSGI_MANIFEST);
        if (manifest != null && deploymentName.endsWith(".jar")) {
            if (ManifestHelper.hasMainAttributeValue(manifest, "Web-ContextPath")) {
                return true;
            }
        }

        // The webbundle scheme can also be used for plain war deployemnts that are then
        // transformed into an OSGi Web Application Bundle (WAB)
        if (deploymentName.startsWith("webbundle:")) {
            return true;
        }

        return false;
    }
}
