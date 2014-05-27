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

import java.net.URI;
import java.net.URL;

import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.servlets.UseStaticMethodServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public abstract class AbstractLibPropertyPermissionReadTestCase {

    protected static final String SFX_GRANT = "-grant";
    protected static final String SFX_LIMITED = "-limited";
    protected static final String SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(AbstractLibPropertyPermissionReadTestCase.class);

    /**
     * Check access to 'java.home' property.
     */
    protected void checkJavaHomeProperty(URL webAppURL, int expectedStatus) throws Exception {
        checkProperty(webAppURL, "java.home", expectedStatus);
    }

    /**
     * Check access to 'os.name' property.
     */
    protected void checkOsNameProperty(URL webAppURL, int expectedStatus) throws Exception {
        checkProperty(webAppURL, "os.name", expectedStatus);
    }

    /**
     * Checks access to a system property on the server using {@link UseStaticMethodServlet}.
     *
     * @param webAppURL
     * @param propertyName
     * @param expectedCode expected HTTP Status code
     * @throws Exception
     */
    protected void checkProperty(final URL webAppURL, final String propertyName, final int expectedCode) throws Exception {
        final URI sysPropUri = new URI(webAppURL.toExternalForm() + UseStaticMethodServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(UseStaticMethodServlet.PARAM_PROPERTY_NAME, propertyName));
        LOGGER.debug("Checking if '" + propertyName + "' property is available: " + sysPropUri);
        Utils.makeCall(sysPropUri, expectedCode);
    }

    /**
     * Create java archive with PropertyReadStaticMethodClass class
     *
     * @return created java archive
     */
    protected static JavaArchive createLibrary() {
        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "library.jar");
        lib.addClasses(PropertyReadStaticMethodClass.class);
        return lib;
    }

}
