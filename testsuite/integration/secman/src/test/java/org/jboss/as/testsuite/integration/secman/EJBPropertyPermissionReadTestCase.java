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
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyBean;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyRemote;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test case, which checks PropertyPermissions assigned to a deployed EJB application.
 * The application try to do a protected action and it should either complete successfully
 * if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author Josef Cacek
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(EJBPropertyPermissionReadTestCase.SystemPropertiesSetup.class)
@RunAsClient
public class EJBPropertyPermissionReadTestCase {

    private static final String EJBAPP_BASE_NAME = "ejb-read-props";
    private static final String EJBAPP_SFX_GRANT = "-grant";
    private static final String EJBAPP_SFX_LIMITED = "-limited";
    private static final String EJBAPP_SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(EJBPropertyPermissionReadTestCase.class);

    @ArquillianResource
    private InitialContext iniCtx;

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
    public void testJavaHomePropertyLimited() throws Exception {
        checkJavaHomeProperty(EJBAPP_SFX_LIMITED, false);
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

    /**
     * Check AS defined (standalone.xml) property access in application, where PropertyPermission for all properties is granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_GRANT)
    public void testASLevelPropertyGrant() throws Exception {
        checkTestProperty(EJBAPP_SFX_GRANT, false);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where not all PropertyPermissions are granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_LIMITED)
    public void testASLevelPropertyLimited() throws Exception {
        checkTestProperty(EJBAPP_SFX_LIMITED, true);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where no PropertyPermission is granted.
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EJBAPP_SFX_DENY)
    public void testASLevelPropertyDeny() throws Exception {
        checkTestProperty(EJBAPP_SFX_DENY, true);
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
     * Check access to {@value #EJBAPP_BASE_NAME} property.
     */
    private void checkTestProperty(final String moduleNameSuffix, final boolean exceptionExpected) throws Exception {
        checkProperty(moduleNameSuffix, EJBAPP_BASE_NAME, exceptionExpected, EJBAPP_BASE_NAME);
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
        ReadSystemPropertyRemote bean = lookupEjb(EJBAPP_BASE_NAME + moduleNameSuffix, ReadSystemPropertyBean.class.getSimpleName(), ReadSystemPropertyRemote.class);
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
                    || !(e instanceof EJBException)
                    || !(e.getCause() instanceof java.security.AccessControlException)) {
                LOGGER.warn("Unexpected exception occurred", e);
                fail("Unexpected exception occurred");
            }
        }
    }

    protected <T> T lookupEjb(final String moduleName, final String beanName, final Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("ejb:/" + moduleName + "//" + beanName + "!" + interfaceType.getName()));
    }

    /**
     * Server setup task, which adds a system property to AS configuration.
     */
    public static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] { new DefaultSystemProperty(EJBAPP_BASE_NAME, EJBAPP_BASE_NAME) };
        }
    }

}
