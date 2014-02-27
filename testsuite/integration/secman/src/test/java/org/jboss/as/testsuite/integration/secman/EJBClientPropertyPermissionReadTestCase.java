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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyBean;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyRemote;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test case, which checks PropertyPermissions assigned to a deployed EJB application
 * getting EJBs via 'ejb:' on the server side. The application try to do a protected
 * action and it should either complete successfully
 * if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
public class EJBClientPropertyPermissionReadTestCase {

    private static final String EJBAPP_BASE_NAME = "ejb-read-props";
    private static final String EJBAPP_SFX_GRANT = "-grant";
    private static final String EJBAPP_SFX_LIMITED = "-limited";
    private static final String EJBAPP_SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(EJBClientPropertyPermissionReadTestCase.class);

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    @Deployment(name = EJBAPP_SFX_GRANT)
    public static JavaArchive grantEjbDeployment() {
        return ejbDeployment(EJBAPP_SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    @Deployment(name = EJBAPP_SFX_LIMITED)
    public static JavaArchive limitedEjbDeployment() {
        return ejbDeployment(EJBAPP_SFX_LIMITED);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    @Deployment(name = EJBAPP_SFX_DENY)
    public static JavaArchive denyEjbDeployment() {
        return ejbDeployment(EJBAPP_SFX_DENY);
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_GRANT)
    @Ignore("Intermitently fails because of missing doPrivileged blocks in EJB client")
    public void testJavaHomePropertyGrant() throws Exception {
        checkJavaHomeProperty(EJBAPP_SFX_GRANT, false);
    }

    /**
     * Check standard java property access in application, where not all PropertyPermissions are granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_LIMITED)
    @Ignore("Intermitently fails because of missing doPrivileged blocks in EJB client")
    public void testJavaHomePropertyLimited() throws Exception {
        checkJavaHomeProperty(EJBAPP_SFX_LIMITED, false);
    }

    /**
     * Check standard java property access in application, where not all PropertyPermissions are granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_LIMITED)
    public void testOsNamePropertyLimited() throws Exception {
        checkOsNameProperty(EJBAPP_SFX_LIMITED, true);
    }

    /**
     * Check standard java property access in application, where no PropertyPermission is granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_DENY)
    public void testJavaHomePropertyDeny() throws Exception {
        checkJavaHomeProperty(EJBAPP_SFX_DENY, true);
    }

    private static JavaArchive ejbDeployment(final String suffix) {
        LOGGER.info("Start EJB deployment");
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, EJBAPP_BASE_NAME + suffix + ".jar");
        ejb.addPackage(ReadSystemPropertyBean.class.getPackage());
        LOGGER.debug(ejb.toString(true));
        return ejb;
    }

    /**
     * Check access to 'java.home' property.
     */
    private void checkJavaHomeProperty(final String moduleNameSuffix, final boolean exceptionExpected) throws Exception {
        checkProperty(moduleNameSuffix, "java.home", exceptionExpected, null);
    }

    /**
     * Check access to 'os.name' property.
     */
    private void checkOsNameProperty(final String moduleNameSuffix, final boolean exceptionExpected) throws Exception {
        checkProperty(moduleNameSuffix, "os.name", exceptionExpected, null);
    }

    /**
     * Checks access to a system property on the server using {@link org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet}.
     *
     * @param moduleNameSuffix
     * @param propertyName
     * @param exceptionExpected expected HTTP Status code
     * @param expectedValue expected response value; if null then response body is not checked
     * @throws Exception
     */
    private void checkProperty(final String moduleNameSuffix, final String propertyName, final boolean exceptionExpected, final String expectedValue)
            throws Exception {
        LOGGER.debug("Checking if '" + propertyName + "' property is available");
        InitialContext ctx = null;
        try {
            ctx = new InitialContext();
            ReadSystemPropertyRemote bean = lookupEjb(ctx, EJBAPP_BASE_NAME + moduleNameSuffix, ReadSystemPropertyBean.class.getSimpleName(), ReadSystemPropertyRemote.class);
            assertNotNull(bean);

            try {
                String propertyValue = bean.readSystemProperty(propertyName);
                if (expectedValue != null) {
                    assertEquals("System property value doesn't match the expected one.", expectedValue, propertyValue);
                }

                if (exceptionExpected) {
                    fail("An exception was expected");
                }
            } catch (Throwable e) {
                if (exceptionExpected == false
                        || !(e instanceof RuntimeException)
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

    protected <T> T lookupEjb(final Context context, final String moduleName, final String beanName, final Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(context.lookup("ejb:/" + moduleName + "//" + beanName + "!" + interfaceType.getName()));
    }

}
