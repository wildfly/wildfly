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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
                     InputStreamReader inputReader = new InputStreamReader(input);
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
