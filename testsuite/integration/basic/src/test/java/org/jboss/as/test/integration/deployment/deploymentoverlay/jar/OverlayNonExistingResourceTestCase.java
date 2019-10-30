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

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OverlayNonExistingResourceTestCase extends JarOverlayTestBase {

    private static final String OVERLAY = "HAL9000";
    private static final String DEPLOYMENT_OVERLAYED = "overlayed";
    private static final String DEPLOYMENT_OVERLAYED_ARCHIVE = DEPLOYMENT_OVERLAYED + ".jar";

    @Deployment(name = DEPLOYMENT_OVERLAYED)
    public static Archive<?> createDeployment() throws Exception {
        return createOverlayedArchive(false, DEPLOYMENT_OVERLAYED_ARCHIVE);
    }

    @Test
    public void testOverlay() throws Exception {
        final InitialContext ctx = getInitialContext();
        try {
            OverlayableInterface iface = (OverlayableInterface) ctx.lookup(getEjbBinding("", DEPLOYMENT_OVERLAYED, "",
                    OverlayEJB.class, OverlayableInterface.class));
            Assert.assertEquals("Overlayed resource does not match pre-overlay expectations!", null, iface.fetchResource());
            Assert.assertEquals("Static resource does not match pre-overlay expectations!", OverlayableInterface.STATIC, iface.fetchResourceStatic());
            setupOverlay(DEPLOYMENT_OVERLAYED_ARCHIVE, OVERLAY, OverlayableInterface.RESOURCE, OverlayableInterface.OVERLAYED);
            Assert.assertEquals("Overlayed resource does not match post-overlay expectations!", OverlayableInterface.OVERLAYED, iface.fetchResource());
            Assert.assertEquals("Static resource does not match post-overlay expectations!", OverlayableInterface.STATIC, iface.fetchResourceStatic());
        } finally {
            try {
                ctx.close();
            } catch (Exception e) {
                LOGGER.error("Closing context failed", e);
            }
            try {
                removeOverlay(DEPLOYMENT_OVERLAYED_ARCHIVE, OVERLAY, OverlayableInterface.RESOURCE);
            } catch (Exception e) {
                LOGGER.error("Removing overlay failed", e);
            }
        }
    }

}
