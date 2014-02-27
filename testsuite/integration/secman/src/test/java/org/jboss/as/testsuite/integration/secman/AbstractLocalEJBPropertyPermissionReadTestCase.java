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

import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyBean;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyLocal;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import javax.ejb.EJBException;
import javax.naming.InitialContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * A common class for PropertyPermissions tests on an EJB accessed locally.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractLocalEJBPropertyPermissionReadTestCase {

    private static final String EJBAPP_BASE_NAME = "ejb-read-props";
    private static final String EJBAPP_SFX_GRANT = "-grant";
    private static final String EJBAPP_SFX_LIMITED = "-limited";
    private static final String EJBAPP_SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(AbstractLocalEJBPropertyPermissionReadTestCase.class);

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    protected static JavaArchive grantEjbDeployment() {
        return ejbDeployment(EJBAPP_SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    protected static JavaArchive limitedEjbDeployment() {
        return ejbDeployment(EJBAPP_SFX_LIMITED);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    protected static JavaArchive denyEjbDeployment() {
        return ejbDeployment(EJBAPP_SFX_DENY);
    }

    private static JavaArchive ejbDeployment(final String suffix) {
        LOGGER.info("Start EJB deployment");
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, EJBAPP_BASE_NAME + suffix + ".jar");
        ejb.addPackage(ReadSystemPropertyBean.class.getPackage());
        ejb.addClass(AbstractLocalEJBPropertyPermissionReadTestCase.class);
        ejb.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        LOGGER.debug(ejb.toString(true));
        return ejb;
    }

    /**
     * Check access to 'java.home' property.
     */
    protected void checkJavaHomeProperty(final ReadSystemPropertyLocal testBean, final boolean exceptionExpected) throws Exception {
        checkProperty(testBean, "java.home", exceptionExpected, null);
    }

    /**
     * Check access to 'os.name' property.
     */
    protected void checkOsNameProperty(final ReadSystemPropertyLocal testBean, final boolean exceptionExpected) throws Exception {
        checkProperty(testBean, "os.name", exceptionExpected, null);
    }

    /**
     * Checks access to a system property on the server using {@link org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet}.
     *
     * @param testBean
     * @param propertyName
     * @param exceptionExpected expected HTTP Status code
     * @param expectedValue expected response value; if null then response body is not checked
     * @throws Exception
     */
    protected void checkProperty(final ReadSystemPropertyLocal testBean, final String propertyName, final boolean exceptionExpected, final String expectedValue)
            throws Exception {
        LOGGER.debug("Checking if '" + propertyName + "' property is available");
        InitialContext ctx = null;
        try {
            ctx = new InitialContext();
            assertNotNull(testBean);

            try {
                String propertyValue = testBean.readSystemProperty(propertyName);
                if (expectedValue != null) {
                    assertEquals("System property value doesn't match the expected one.", expectedValue, propertyValue);
                }

                if (exceptionExpected) {
                    fail("An exception was expected");
                }
            } catch (Throwable e) {
                if (exceptionExpected == false
                        || !(e instanceof EJBException)
                        || !(e.getCause() instanceof java.security.AccessControlException)) {
                    LOGGER.warn("Unexpected exception occurred", e);
                    fail("Unexpected exception occurred");
                }
            }
        } finally {
            if (ctx != null)
                ctx.close();
        }
    }

}
