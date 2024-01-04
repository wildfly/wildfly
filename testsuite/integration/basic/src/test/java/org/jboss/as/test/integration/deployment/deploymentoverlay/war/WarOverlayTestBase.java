/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.deploymentoverlay.war;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.JarOverlayTestBase;
import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.OverlayableInterface;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author baranowb
 * @author lgao
 *
 */
public class WarOverlayTestBase extends JarOverlayTestBase{

    public static final String OVERLAY_HTML = "overlay.html";
    public static final String STATIC_HTML = "static.html";

    public static Archive<?> createWARWithOverlayedArchive(final boolean resourcePresent, String deploymentOverlayedArchive, final String deploymentTopArchve){
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentTopArchve);
        Archive<?> jar = createOverlayedArchive(resourcePresent,deploymentOverlayedArchive);
        if (resourcePresent) {
            war.add(new StringAsset(OverlayableInterface.ORIGINAL), OVERLAY_HTML);
        }
        war.add(new StringAsset(OverlayableInterface.STATIC), STATIC_HTML);
        war.addAsLibrary(jar);
        return war;
    }

    public static String readContent(String url) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            conn.connect();
            int httpCode = conn.getResponseCode();
            if (httpCode == 200) {
                try (InputStream input = conn.getInputStream();
                     InputStreamReader inputReader = new InputStreamReader(input, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(inputReader)) {
                    return reader.readLine();
                }
            } else if (httpCode == 404) {
                return null;
            }
            throw new RuntimeException("Un expected response code: " + httpCode);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}
