/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.secman;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.servlets.ForwardServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * Test that the servlet should be able to forward Jakarta Server Pages resource within same deployment.
 *
 * @author Lin Gao
 */
@RunWith(Arquillian.class)
public class ForwardJSPTestCase {

    private static final String APP_NAME = "forward";

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_NAME, testable = false)
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        war.addClasses(ForwardServlet.class);
        war.addAsWebResource(ForwardServlet.class.getPackage(), "forward.jsp", "forward.jsp");
        return war;
    }

    @Test
    public void testForwardResource(@ArquillianResource URL webAppURL) throws Exception {
        final URI sysPropUri = new URI(webAppURL.toExternalForm() + ForwardServlet.SERVLET_PATH.substring(1));
        final String respBody = Utils.makeCall(sysPropUri, 200);
        assertTrue("jsp should be forwarded", respBody.contains("This is the forward.jsp"));
    }

}
