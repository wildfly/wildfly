package org.jboss.as.test.manualmode.server.graceless;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.manualmode.server.graceless.deploymenta.TestApplicationA;
import org.jboss.as.test.manualmode.server.graceless.deploymentb.TestApplicationB;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
//@ServerSetup(JdbcRepositoryTestCase.AgroalJdbcJobRepositorySetUp.class)
public class GracelessStartTestCase {
    private static final String CONTAINER = "graceless-server";
    private static final String DEPLOYMENTA = "deploymenta";
    private static final String DEPLOYMENTB = "deploymentb";

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = DEPLOYMENTA, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeploymentA() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENTA + ".war");
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        war.addPackage(TestApplicationA.class.getPackage());
        return war;
    }

    @Deployment(name = DEPLOYMENTB, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeploymentB() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENTB + ".war");
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        war.addPackage(TestApplicationB.class.getPackage());
        return war;
    }

    @Test
//    @RunAsClient
    public void testGracelessDeployment() {
        System.out.println("***** [graceless] Starting the container");
        containerController.start(CONTAINER);

        System.out.println("***** [graceless] Deploying " + DEPLOYMENTA);
        deployer.deploy(DEPLOYMENTA);
        System.out.println("***** [graceless] Successfully deployed " + DEPLOYMENTA);

        System.out.println("***** [graceless] Deploying " + DEPLOYMENTB);
        deployer.deploy(DEPLOYMENTB);
        System.out.println("***** [graceless] Successfully deployed " + DEPLOYMENTB);

        containerController.stop(CONTAINER);

        System.out.println("***** [graceless] Restarting the container");
        containerController.start(CONTAINER); //, config);
        System.out.println("***** [graceless] Stopping the container");
        containerController.stop(CONTAINER);
    }
}