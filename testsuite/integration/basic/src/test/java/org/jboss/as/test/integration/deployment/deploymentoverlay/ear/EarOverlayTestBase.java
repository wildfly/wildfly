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

package org.jboss.as.test.integration.deployment.deploymentoverlay.ear;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.deployment.deploymentoverlay.jar.OverlayableInterface;
import org.jboss.as.test.integration.deployment.deploymentoverlay.war.WarOverlayTestBase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author baranowb
 * @author lgao
 *
 */
public class EarOverlayTestBase extends WarOverlayTestBase {

    public static final String WEB = "web";
    public static final String WEB_ARCHIVE = WEB + ".war";
    public static final String WEB_OVERLAY = WEB_ARCHIVE + "/" + OVERLAY_HTML;

    public static final String RESOURCE_IN_JAR_OVERLAY = "jar/overlay.txt";
    public static final String META_RESOURCE_IN_JAR_OVERLAY = "META-INF/" + RESOURCE_IN_JAR_OVERLAY;

    public static final String RESOURCE_IN_JAR_STATIC = "jar/static.txt";
    public static final String META_RESOURCE_IN_JAR_STATIC = "META-INF/" + RESOURCE_IN_JAR_STATIC;

    public static final String SERVLETS_JAR = "echoservlet.jar";

    public static final String JAR_IN_WAR_OVERLAY_PATH = WEB_ARCHIVE + "/" + SERVLETS_JAR + "/" + META_RESOURCE_IN_JAR_OVERLAY;

    public static Archive<?> createEARWithOverlayedArchive(final boolean resourcePresent, String deploymentOverlayedArchive, final String deploymentTopArchve){
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deploymentTopArchve);
                Archive<?> jar = createOverlayedArchive(resourcePresent,deploymentOverlayedArchive);
        ear.addAsModule(jar);
        WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_ARCHIVE);
        war.add(new StringAsset(OverlayableInterface.STATIC), STATIC_HTML);
        if (resourcePresent) {
            war.add(new StringAsset(OverlayableInterface.ORIGINAL), OVERLAY_HTML);
        }
        JavaArchive servlets = ShrinkWrap.create(JavaArchive.class, SERVLETS_JAR);
        servlets.addClasses(EchoStaticServlet.class, EchoOverlayServlet.class);
        servlets.addAsManifestResource(new StringAsset(OverlayableInterface.STATIC), RESOURCE_IN_JAR_STATIC);
        if (resourcePresent) {
            servlets.addAsManifestResource(new StringAsset(OverlayableInterface.ORIGINAL), RESOURCE_IN_JAR_OVERLAY);
        }
        war.addAsLibraries(servlets);
        ear.addAsModule(war);
        return ear;
    }

    @SuppressWarnings("serial")
    @WebServlet("/echoStatic")
    public static class EchoStaticServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(META_RESOURCE_IN_JAR_STATIC);
                 InputStreamReader inputReader = new InputStreamReader(input);
                 BufferedReader reader = new BufferedReader(inputReader)) {
                resp.getWriter().write(reader.readLine());
                resp.flushBuffer();
            }
        }
    }

    @SuppressWarnings("serial")
    @WebServlet("/echoOverlay")
    public static class EchoOverlayServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(META_RESOURCE_IN_JAR_OVERLAY)) {
                if (input == null) {
                    resp.sendError(404);
                    return;
                }
                try (InputStreamReader inputReader = new InputStreamReader(input);
                     BufferedReader reader = new BufferedReader(inputReader)) {
                    resp.getWriter().write(reader.readLine());
                    resp.flushBuffer();
                }
            }
        }
    }

}
