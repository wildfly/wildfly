/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marks CapeDwarf deployment / app.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfDeploymentMarker {
    private static final AttachmentKey<CapedwarfDeploymentMarker> MARKER = AttachmentKey.create(CapedwarfDeploymentMarker.class);

    private boolean bundledAppEngineApi;
    private boolean cdiApp;
    private String appId;
    private Set<String> persistenceProviders;

    private CapedwarfDeploymentMarker() {
    }

    /**
     * Mark the top level deployment as being a CapeDwarf deployment.
     *
     * @param unit the deployment unit
     */
    static void mark(DeploymentUnit unit) {
        unit.putAttachment(MARKER, new CapedwarfDeploymentMarker());
    }

    /**
     * retuns true if the {@link DeploymentUnit} is a GAE app -- has appengine-web.xml in WEB-INF,
     * while it's a single .war deployment.
     *
     * @param unit the deployment unit
     * @return true if CapeDwarf deployment, false otherwise
     */
    public static boolean isCapedwarfDeployment(DeploymentUnit unit) {
        return unit.hasAttachment(MARKER);
    }

    /**
     * Keep info weather GAE api is bundled.
     *
     * @param unit the deployment unit
     */
    public static void setBundledAppEngineApi(DeploymentUnit unit) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        if (marker != null)
            marker.bundledAppEngineApi = true;
    }

    /**
     * Is GAE api bundled in app.
     *
     * @param unit the deployment unit
     * @return true if GAE api is bundled, false otherwise
     */
    public static boolean isBundledAppEngineApi(DeploymentUnit unit) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        return marker != null && marker.bundledAppEngineApi;
    }

    /**
     * Keep info weather app was CDI originally.
     *
     * @param unit the deployment unit
     * @param flag the cdi app flag
     */
    public static void setCDIApp(DeploymentUnit unit, boolean flag) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        if (marker != null)
            marker.cdiApp = flag;
    }

    /**
     * Was this originally CDI app.
     *
     * @param unit the deployment unit
     * @return true if app was originally CDI app, false otherwise
     */
    public static boolean isCDIApp(DeploymentUnit unit) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        return marker != null && marker.cdiApp;
    }

    /**
     * Set app id info.
     *
     * @param unit  the deployment unit
     * @param appId the app id
     */
    public static void setAppId(DeploymentUnit unit, String appId) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        if (marker != null)
            marker.appId = appId;
    }

    /**
     * Get app id.
     *
     * @param unit the deployment unit
     * @return app id
     */
    public static String getAppId(DeploymentUnit unit) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        return marker != null ? marker.appId : null;
    }

    /**
     * Add persistence provider.
     *
     * @param unit                the deployment unit
     * @param persistenceProvider the persistence provider
     */
    public static void addPersistenceProvider(DeploymentUnit unit, String persistenceProvider) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        if (marker != null) {
            if (marker.persistenceProviders == null)
                marker.persistenceProviders = new HashSet<String>();
            marker.persistenceProviders.add(persistenceProvider);
        }
    }

    /**
     * Get persistence providers.
     *
     * @param unit the deployment unit
     * @return the persistence providers or empty set if none
     */
    public static Set<String> getPersistenceProviders(DeploymentUnit unit) {
        final CapedwarfDeploymentMarker marker = unit.getAttachment(MARKER);
        return marker != null ? marker.persistenceProviders : Collections.<String>emptySet();
    }
}
