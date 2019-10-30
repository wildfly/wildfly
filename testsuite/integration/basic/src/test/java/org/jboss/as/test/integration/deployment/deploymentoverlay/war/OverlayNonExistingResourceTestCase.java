/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.deploymentoverlay.war;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.OverlayEJB;
import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.OverlayableInterface;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 * @author lgao
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OverlayNonExistingResourceTestCase extends WarOverlayTestBase {
    private static final String OVERLAY = "HAL9000";
    private static final String DEPLOYMENT_OVERLAYED = "overlayed";
    private static final String DEPLOYMENT_OVERLAYED_ARCHIVE = DEPLOYMENT_OVERLAYED + ".jar";

    private static final String DEPLOYMENT_SHELL = "shell";
    private static final String DEPLOYMENT_SHELL_ARCHIVE = DEPLOYMENT_SHELL + ".war";

    private static final String RESOURCE = "/WEB-INF/lib/"+DEPLOYMENT_OVERLAYED_ARCHIVE+"//"+OverlayableInterface.RESOURCE;

    @Deployment(name = DEPLOYMENT_SHELL)
    public static Archive<?> createDeployment() throws Exception {
        return createWARWithOverlayedArchive(false, DEPLOYMENT_OVERLAYED_ARCHIVE,DEPLOYMENT_SHELL_ARCHIVE);
    }

    @Test
    public void testOverlay() throws Exception {
        final InitialContext ctx = getInitialContext();
        Map<String, String> overlay = new HashMap<String, String>();
        try{
        OverlayableInterface iface = (OverlayableInterface) ctx.lookup(getEjbBinding("", DEPLOYMENT_SHELL, "",
                OverlayEJB.class, OverlayableInterface.class));
        Assert.assertEquals("Overlayed resource in war/jar does not match pre-overlay expectations!", null, iface.fetchResource());
        Assert.assertEquals("Static resource in war/jar does not match pre-overlay expectations!", OverlayableInterface.STATIC, iface.fetchResourceStatic());

        Assert.assertEquals("HTML resource in war does not match pre-overlay expectations!", null,
                readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + OVERLAY_HTML));
        Assert.assertEquals("HTML Static resource in war does not match pre-overlay expectations!", OverlayableInterface.STATIC,
                readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + STATIC_HTML));

        overlay.put(RESOURCE, OverlayableInterface.OVERLAYED);
        overlay.put(OVERLAY_HTML, OverlayableInterface.OVERLAYED);
        setupOverlay(DEPLOYMENT_SHELL_ARCHIVE, OVERLAY, overlay);

        Assert.assertEquals("Overlayed resource in war/jar does not match post-overlay expectations!", OverlayableInterface.OVERLAYED, iface.fetchResource());
        Assert.assertEquals("Static resource in war/jar does not match post-overlay expectations!", OverlayableInterface.STATIC, iface.fetchResourceStatic());

        Assert.assertEquals("HTML static resource in war does not match post-overlay expectations!", OverlayableInterface.STATIC,
                readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + STATIC_HTML));
        Assert.assertEquals("HTML resource in war does not match post-overlay expectations!", OverlayableInterface.OVERLAYED,
                readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + OVERLAY_HTML));

        } finally {
            try {
                ctx.close();
            } catch (Exception e) {
                LOGGER.error("Closing context failed", e);
            }
            try {
                removeOverlay(DEPLOYMENT_SHELL_ARCHIVE, OVERLAY, overlay.keySet());
            } catch (Exception e) {
                LOGGER.error("Removing overlay failed", e);
            }
        }
    }
}
