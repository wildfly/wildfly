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
package org.jboss.as.test.integration.security.xacml;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests JBossPDP access from a web-application (servlet).
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
public class JBossPDPServletInitializationTestCase {

    private static Logger LOGGER = Logger.getLogger(JBossPDPServletInitializationTestCase.class);

    // Public methods --------------------------------------------------------

    /**
     * Creates {@link WebArchive} for the deployment.
     *
     * @return
     */
    @Deployment(testable = false)
    public static WebArchive deploymentWar() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "pdp-service-bean.war");
        war.addClass(JBossPDPInitServlet.class);
        XACMLTestUtils.addCommonClassesToArchive(war);
        XACMLTestUtils.addJBossDeploymentStructureToArchive(war);
        XACMLTestUtils.addXACMLPoliciesToArchive(war);
        return war;
    }

    /**
     * Validates that the servlet returns "OK" response.
     *
     * @throws Exception
     */
    @Test
    public void testPdpServlet(@ArquillianResource final URL webAppURL) throws Exception {
        assertEquals(JBossPDPInitServlet.RESPONSE_OK,
                HttpRequest.get(webAppURL.toExternalForm() + JBossPDPInitServlet.SERVLET_PATH.substring(1), 10, SECONDS));
    }
}