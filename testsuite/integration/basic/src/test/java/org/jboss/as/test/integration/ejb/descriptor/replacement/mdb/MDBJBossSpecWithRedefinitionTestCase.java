/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.mdb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rhatlapa
 */
@RunWith(Arquillian.class)
@ServerSetup({org.jboss.as.test.integration.ejb.descriptor.replacement.mdb.MDBJBossSpecWithRedefinitionTestCase.JmsQueueSetup.class})
public class MDBJBossSpecWithRedefinitionTestCase extends MDBTesting {

    @Deployment(name = DEPLOYMENT_WITH_REDEFINITION)
    public static Archive getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb.jar");
        ejbJar.addPackage(SimpleMessageDrivenBean.class.getPackage());
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addClass(org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil.class);
        ejbJar.addClass(org.jboss.as.test.integration.ejb.descriptor.replacement.mdb.MDBJBossSpecTestCase.JmsQueueSetup.class);
        ejbJar.addClass(TimeoutUtil.class);
        ejbJar.addAsManifestResource(MDBJBossSpecTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ejbJar.addAsManifestResource(MDBJBossSpecTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        return ejbJar;
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
    public void testMDBWithJBossSpecRedefinition() throws Exception {
        testMDB("JBossSpecWithRedefinition standardQueue", "JBossSpecWithRedefinition redefinedQueue");
    }
}
