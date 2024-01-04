/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
