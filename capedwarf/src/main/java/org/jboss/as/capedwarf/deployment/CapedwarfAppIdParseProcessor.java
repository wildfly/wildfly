/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;

/**
 * Parse app id.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfAppIdParseProcessor extends CapedwarfDeploymentUnitProcessor {
    private static final String APPENGINE_WEB_XML = "WEB-INF/appengine-web.xml";
    private static final String APPLICATION = "<application>";

    private Set<String> apps = new ConcurrentSkipListSet<String>();

    protected void doDeploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        ResourceRoot deploymentRoot = unit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile root = deploymentRoot.getRoot();
        VirtualFile xml = root.getChild(APPENGINE_WEB_XML);

        String appId = parseAppId(xml);

        if (apps.add(appId) == false)
            throw new IllegalArgumentException("App id already exists: " + appId);

        CapedwarfDeploymentMarker.setAppId(unit, appId);
    }

    protected String parseAppId(VirtualFile xml) throws DeploymentUnitProcessingException {
        try {
            InputStream is = xml.openStream();
            try {
                StringBuilder builder = new StringBuilder();
                int x;
                boolean isAppId = false;
                StringBuilder appId = new StringBuilder();
                while ((x = is.read()) != -1) {
                    char ch = (char) x;
                    if (isAppId) {
                        if (ch == '<')
                            break;
                        else
                            appId.append(ch);
                    } else {
                        builder.append(ch);
                    }
                    if (isAppId == false && builder.toString().endsWith(APPLICATION)) {
                        isAppId = true;
                    }
                }
                return appId.toString();
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    @Override
    protected void doUndeploy(DeploymentUnit unit) {
        String appId = CapedwarfDeploymentMarker.getAppId(unit);
        apps.remove(appId);
    }
}
