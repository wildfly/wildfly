/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author baranowb
 */
@ExtendWith(ArquillianExtension.class)
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
            Assertions.assertEquals(null, iface.fetchResource(), "Overlayed resource does not match pre-overlay expectations!");
            Assertions.assertEquals(OverlayableInterface.STATIC, iface.fetchResourceStatic(), "Static resource does not match pre-overlay expectations!");
            setupOverlay(DEPLOYMENT_OVERLAYED_ARCHIVE, OVERLAY, OverlayableInterface.RESOURCE, OverlayableInterface.OVERLAYED);
            Assertions.assertEquals(OverlayableInterface.OVERLAYED, iface.fetchResource(), "Overlayed resource does not match post-overlay expectations!");
            Assertions.assertEquals(OverlayableInterface.STATIC, iface.fetchResourceStatic(), "Static resource does not match post-overlay expectations!");
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
