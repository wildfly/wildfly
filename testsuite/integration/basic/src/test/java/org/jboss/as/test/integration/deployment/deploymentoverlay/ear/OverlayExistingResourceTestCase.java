/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.deploymentoverlay.ear;

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
public class OverlayExistingResourceTestCase extends EarOverlayTestBase {
    private static final String OVERLAY = "HAL9000";
    private static final String DEPLOYMENT_OVERLAYED = "overlayed";
    private static final String DEPLOYMENT_OVERLAYED_ARCHIVE = DEPLOYMENT_OVERLAYED + ".jar";

    private static final String DEPLOYMENT_SHELL = "shell";
    private static final String DEPLOYMENT_SHELL_ARCHIVE = DEPLOYMENT_SHELL + ".ear";

    private static final String RESOURCE = "/"+DEPLOYMENT_OVERLAYED_ARCHIVE+"//"+OverlayableInterface.RESOURCE;

    @Deployment(name = DEPLOYMENT_SHELL)
    public static Archive<?> createDeployment() throws Exception {
        return createEARWithOverlayedArchive(true, DEPLOYMENT_OVERLAYED_ARCHIVE,DEPLOYMENT_SHELL_ARCHIVE);
    }

    @Test
    public void testOverlay() throws Exception {
        final InitialContext ctx = getInitialContext();
        Map<String, String> overlay = new HashMap<String, String>();
        try{
            OverlayableInterface iface = (OverlayableInterface) ctx.lookup(getEjbBinding(DEPLOYMENT_SHELL, DEPLOYMENT_OVERLAYED, "",
                    OverlayEJB.class, OverlayableInterface.class));
            Assert.assertEquals("Overlayed resource does not match pre-overlay expectations!", OverlayableInterface.ORIGINAL, iface.fetchResource());
            Assert.assertEquals("Static resource does not match pre-overlay expectations!", OverlayableInterface.STATIC, iface.fetchResourceStatic());

            Assert.assertEquals("HTML resource in ear/war does not match pre-overlay expectations!", OverlayableInterface.ORIGINAL,
                    readContent(managementClient.getWebUri() + "/" + WEB + "/" + OVERLAY_HTML));
            Assert.assertEquals("HTML Static in ear/war resource does not match pre-overlay expectations!", OverlayableInterface.STATIC,
                    readContent(managementClient.getWebUri() + "/" + WEB + "/" + STATIC_HTML));

            Assert.assertEquals("Static resource in ear/war/jar does not match pre-overlay expectations!", OverlayableInterface.STATIC,
                    readContent(managementClient.getWebUri() + "/" + WEB + "/echoStatic"));
            Assert.assertEquals("Resource in ear/war/jar does not match pre-overlay expectations!", OverlayableInterface.ORIGINAL,
                    readContent(managementClient.getWebUri() + "/" + WEB + "/echoOverlay"));

            overlay.put(RESOURCE, OverlayableInterface.OVERLAYED);
            overlay.put(WEB_OVERLAY, OverlayableInterface.OVERLAYED);
            setupOverlay(DEPLOYMENT_SHELL_ARCHIVE, OVERLAY, overlay);

            Assert.assertEquals("Overlayed resource in ear/jar does not match post-overlay expectations!", OverlayableInterface.OVERLAYED, iface.fetchResource());
            Assert.assertEquals("Static resource in ear/jar does not match post-overlay expectations!", OverlayableInterface.STATIC, iface.fetchResourceStatic());

            Assert.assertEquals("HTML static resource in ear/war does not match post-overlay expectations!", OverlayableInterface.STATIC,
                    readContent(managementClient.getWebUri() + "/" + WEB + "/" + STATIC_HTML));
            Assert.assertEquals("HTML resource in ear/war does not match post-overlay expectations!", OverlayableInterface.OVERLAYED,
                    readContent(managementClient.getWebUri() + "/" + WEB + "/" + OVERLAY_HTML));

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
