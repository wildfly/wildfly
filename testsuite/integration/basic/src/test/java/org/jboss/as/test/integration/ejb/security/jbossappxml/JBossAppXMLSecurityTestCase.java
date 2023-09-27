/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.jbossappxml;

import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test the security mappings from jboss-app.xml
 * @author anil saldhana
 */
@RunWith(Arquillian.class)
@RunAsClient
@SuppressWarnings({"rawtypes","unchecked"})
@ServerSetup(JBossAppXMLSecurityTestCase.JBossAppXMLSecurityTestCaseSetup.class)
@Category(CommonCriteria.class)
public class JBossAppXMLSecurityTestCase {
    private static final String APP_NAME = "myapp";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "jboss-app-xml-sec";
    private static final Logger logger = Logger.getLogger(JBossAppXMLSecurityTestCase.class);

    @ArquillianResource
    private Context context;

    static class JBossAppXMLSecurityTestCaseSetup extends EjbSecurityDomainSetup {
        @Override
        public boolean isUsersRolesRequired() {
            return false;
        }

        @Override
        protected String getSecurityDomainName() {
            return "mydomain";
        }
    }

    @Deployment(testable = false) // the incorrectly named "testable" attribute tells Arquillian whether or not
    // it should add Arquillian specific metadata to the archive (which ultimately transforms it to a WebArchive).
    // We don't want that, so set that flag to false
    public static Archive createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        ear.addAsManifestResource(JBossAppXMLSecurityTestCase.class.getPackage(),
                "jboss-app.xml", "jboss-app.xml");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(BeanInterface.class,FirstBean.class, SecondBean.class);
        jar.addPackage(CommonCriteria.class.getPackage());
        jar.addAsManifestResource(JBossAppXMLSecurityTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");

        ear.addAsModule(jar);
        return ear;
    }

    @Test
    public void testEJBRunAs() throws Exception {
        final BeanInterface firstBean = (BeanInterface) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + FirstBean.class.getSimpleName() + "!" + BeanInterface.class.getName());
        assertNotNull("Lookup returned a null bean proxy", firstBean);
        String callerPrincipal = firstBean.getCallerPrincipal();
        assertNotNull(callerPrincipal);
        assertEquals("javajoe", callerPrincipal);
        assertTrue(firstBean.isCallerInRole("Manager"));
    }
}