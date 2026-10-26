/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.deploymentoverlay.war;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.OverlayEJB;
import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.OverlayableInterface;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author baranowb
 * @author lgao
 *
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(null, iface.fetchResource(), "Overlayed resource in war/jar does not match pre-overlay expectations!");
        Assertions.assertEquals(OverlayableInterface.STATIC, iface.fetchResourceStatic(), "Static resource in war/jar does not match pre-overlay expectations!");

        Assertions.assertEquals(null, readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + OVERLAY_HTML),
                "HTML resource in war does not match pre-overlay expectations!");
        Assertions.assertEquals(OverlayableInterface.STATIC, readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + STATIC_HTML),
                "HTML Static resource in war does not match pre-overlay expectations!");

        overlay.put(RESOURCE, OverlayableInterface.OVERLAYED);
        overlay.put(OVERLAY_HTML, OverlayableInterface.OVERLAYED);
        setupOverlay(DEPLOYMENT_SHELL_ARCHIVE, OVERLAY, overlay);

        Assertions.assertEquals(OverlayableInterface.OVERLAYED, iface.fetchResource(), "Overlayed resource in war/jar does not match post-overlay expectations!");
        Assertions.assertEquals(OverlayableInterface.STATIC, iface.fetchResourceStatic(), "Static resource in war/jar does not match post-overlay expectations!");

        Assertions.assertEquals(OverlayableInterface.STATIC, readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + STATIC_HTML),
                "HTML static resource in war does not match post-overlay expectations!");
        Assertions.assertEquals(OverlayableInterface.OVERLAYED, readContent(managementClient.getWebUri() + "/" + DEPLOYMENT_SHELL + "/" + OVERLAY_HTML),
                "HTML resource in war does not match post-overlay expectations!");

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
