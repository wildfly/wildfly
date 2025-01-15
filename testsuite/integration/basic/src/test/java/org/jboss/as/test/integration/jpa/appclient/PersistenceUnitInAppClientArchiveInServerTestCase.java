/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.appclient;

import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Cover testing of https://issues.redhat.com/browse/WFLY-20277 change to ignore persistence units in app-client container archive when deploying on server.
 *
 * This test includes a client container archive that has a persistence.xml with invalid persistence provider name that would produce a deployment failure
 * if the the persistence unit was actually started (since the invalid persistence provider class does not exist).
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PersistenceUnitInAppClientArchiveInServerTestCase {

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "PersistenceUnitInAppClientArchiveInServerTestCase.ear");

        JavaArchive clientModule = ShrinkWrap.create(JavaArchive.class,"appclientcontainerarchive.jar");
        clientModule.addAsManifestResource( PersistenceUnitInAppClientArchiveInServerTestCase.class.getPackage(), "application-client.xml","application-client.xml");
        clientModule.addAsManifestResource(PersistenceUnitInAppClientArchiveInServerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsModule(clientModule);
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarfile.jar");
        jar.addClass(MainArchiveEntity.class);
        ear.addAsLibrary(jar);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "Test.war");
        war.addClasses(HttpRequest.class, SimpleServlet.class);
        // WEB-INF/classes is implied
        war.addAsWebInfResource(PersistenceUnitInAppClientArchiveInServerTestCase.class.getPackage(), "web.xml", "web.xml");
        ear.addAsModule(war);

        return ear;
    }

    /**
     * Any deployment failures for this test means the fix for WFLY-20277 has been broken.
     * This test is expected to simply pass meaning that the test deployment succeeded with the ignored invalid persistence provider.
     */
    @Test
    public void testNothing() throws NamingException {
        assertTrue("if we don't get a deployer failure than this test has passed", true);
    }

}
