/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.servlets.ReadFileServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks JSM permissions to access a file on a file system. Deployed applications try to do a protected action
 * and it should either complete successfully if {@link java.io.FilePermission} is granted, or fail.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FilePermissionTestCase {

    private static final String WEBAPP_BASE_NAME = "read-file";
    private static final String WEBAPP_SFX_GRANT = "-grant";
    private static final String WEBAPP_SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(FilePermissionTestCase.class);

    // Public methods --------------------------------------------------------

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_SFX_GRANT, testable = false)
    public static WebArchive grantDeployment() {
        return warDeployment(WEBAPP_SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_SFX_DENY, testable = false)
    public static WebArchive denyDeployment() {
        return warDeployment(WEBAPP_SFX_DENY);
    }

    /**
     * Checks granted FilePermission with "read" action.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_GRANT)
    public void testFileReadGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkFilePermission(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Checks not-granted FilePermission.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_DENY)
    public void testFileReadDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkFilePermission(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private static WebArchive warDeployment(final String suffix) {
        LOGGER.info("Start WAR deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_BASE_NAME + suffix + ".war");
        war.addClasses(ReadFileServlet.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(war.toString(true));
        }
        return war;
    }

    private void checkFilePermission(URL webAppURL, int expectedStatus) throws Exception {
        // create temporary file
        File tempFile = File.createTempFile("file-permission", ".test");
        tempFile.deleteOnExit();
        FileUtils.writeStringToFile(tempFile, WEBAPP_BASE_NAME, Utils.UTF_8);

        final String path = tempFile.getAbsolutePath();
        LOGGER.debug("Try to read file content from deployed application: " + path);
        final URI uri = new URI(webAppURL.toExternalForm() + ReadFileServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(ReadFileServlet.PARAM_FILE_NAME, path));
        final int expectedResponseCode = expectedStatus;
        try {
            // check if the ReadFileServlet deployed on server has access to the new file
            final String responseBody = Utils.makeCall(uri, expectedResponseCode);
            // if successfully finished, then check if the response body contains the tempfile content
            if (HttpServletResponse.SC_OK == expectedResponseCode) {
                assertEquals("Unexpected file content", WEBAPP_BASE_NAME, responseBody);
            }
        } finally {
            tempFile.delete();
        }
    }
}
