/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

import static org.junit.Assert.assertTrue;

/**
 * Tests if EJB2 application shows the correct user when getCallerPrincipal method is used inside the EJB.
 * Test for [ WFLY-12301 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(Ejb2GetCallerPrincipalServerSetupTask.class)
public class Ejb2GetCallerPrincipalTestCase {

    private static final String DEPLOYMENT = "DEPLOYMENT";
    private static final String ROLE1 = "Users";

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT + ".ear");

        JavaArchive ejb2 = ShrinkWrap.create(JavaArchive.class, "ejb2.jar");
        ejb2.addClasses(TestEJB2.class, TestEJB2Bean.class, TestEJB2Int.class, TestEJB2Home.class);
        ejb2.addAsManifestResource(Ejb2GetCallerPrincipalTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ear.addAsModule(ejb2);

        JavaArchive ejb3 = ShrinkWrap.create(JavaArchive.class, "ejb3.jar");
        ejb3.addClasses(TestEJB3Remote.class, TestEJB3.class);
        ear.addAsModule(ejb3);

        return ear;
    }

    @Test
    public void testEjb2GetCallerPrincipal() throws Exception {
        Context ctx = getInitialContext();

        TestEJB3Remote ejb3 = (TestEJB3Remote) ctx.lookup("ejb:DEPLOYMENT/ejb3/TestEJB3!org.jboss.as.test.integration.ejb.stateless.callerprincipal.TestEJB3Remote");
        assertTrue("Caller must be user1 and must be in the role Users.", ejb3.isCallerInRole(ROLE1));

        TestEJB2 ejb2X = (TestEJB2) ctx.lookup("ejb:DEPLOYMENT/ejb2/TestEJB2Bean!org.jboss.as.test.integration.ejb.stateless.callerprincipal.TestEJB2");
        assertTrue("Caller must be user1 and must be in the role Users.", ejb2X.isCallerInRole(ROLE1));

        TestEJB2Home ejb2Home = (TestEJB2Home) ctx.lookup("ejb:DEPLOYMENT/ejb2/TestEJB2Bean!org.jboss.as.test.integration.ejb.stateless.callerprincipal.TestEJB2Home");
        TestEJB2 ejb2 = ejb2Home.create();
        assertTrue("Caller must be user1 and must be in the role Users.", ejb2.isCallerInRole(ROLE1));
    }

    private InitialContext getInitialContext() throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.SECURITY_PRINCIPAL, "user1");
        jndiProperties.put(Context.SECURITY_CREDENTIALS, "password1");
        jndiProperties.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + TestSuiteEnvironment.getHttpPort());
        return new InitialContext(jndiProperties);
    }
}
