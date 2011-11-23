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

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marks CapeDwarf deployment / app.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfDeploymentMarker {
    private static final AttachmentKey<Boolean> MARKER = AttachmentKey.create(Boolean.class);

    /**
     * Mark the top level deployment as being a CapeDwarf deployment.
     *
     * @param unit the deployment unit
     */
    static void mark(DeploymentUnit unit) {
        unit.putAttachment(MARKER, Boolean.TRUE);
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
}
