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
@ServerSetup({org.jboss.as.test.integration.ejb.descriptor.replacement.mdb.MDBJBossSpecTestCase.JmsQueueSetup.class})
public class MDBJBossSpecTestCase extends MDBTesting {

    
    @Deployment(name = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public static Archive getDeploymentJbossSpec() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb-jboss-spec.jar");
        ejbJar.addPackage(SimpleMessageDrivenBean.class.getPackage());
        ejbJar.addPackage(JMSOperations.class.getPackage());
        ejbJar.addClass(TimeoutUtil.class);
        ejbJar.addClass(org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil.class);
        ejbJar.addClass(org.jboss.as.test.integration.ejb.descriptor.replacement.mdb.MDBJBossSpecTestCase.JmsQueueSetup.class);
        ejbJar.addAsManifestResource(MDBJBossSpecTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        return ejbJar;
    }
    
    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testMDBAsDefinedByJbossSpecDescriptor() throws Exception {
        testMDB("JBossSpec standardQueue", "JBossSpec redefinedQueue");
    }
}
