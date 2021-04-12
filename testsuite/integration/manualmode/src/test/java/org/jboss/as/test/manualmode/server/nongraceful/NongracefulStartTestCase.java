package org.jboss.as.test.manualmode.server.nongraceful;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.manualmode.server.nongraceful.deploymenta.TestApplicationA;
import org.jboss.as.test.manualmode.server.nongraceful.deploymentb.TestApplicationB;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will exercise the non-graceful startup feature of WildFly. This test will deploy two applications,
 * ApplicationA and ApplicationB. ApplicationB, however, while it is starting, tries to access ApplicationA.
 * If the server has started in normal mode, no requests will be allowed until the server has completely
 * started, which will cause the deployment of ApplicationB to fail, as it will block indefinitely. We start the
 * container, though, with --graceful-startup=false, which will allow requests to be processed or rejected cleanly
 * during the server startup process.
 */
@RunWith(Arquillian.class)
public class NongracefulStartTestCase {
    private static final String CONTAINER = "non-graceful-server";
    private static final String DEPLOYMENTA = "deploymenta";
    private static final String DEPLOYMENTB = "deploymentb";
    private static final Logger logger = Logger.getLogger(NongracefulStartTestCase.class);

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = DEPLOYMENTA, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeploymentA() {
        return buildBaseArchive(DEPLOYMENTA)
                .addPackage(TestApplicationA.class.getPackage());
    }

    @Deployment(name = DEPLOYMENTB, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> getDeploymentB() {
        return buildBaseArchive(DEPLOYMENTB)
                .addPackage(TestApplicationB.class.getPackage());
    }

    private static WebArchive buildBaseArchive(String name) {
        return ShrinkWrap
                .create(WebArchive.class, name + ".war")
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        // Required for the ClientBuilder.newBuilder() so the ServiceLoader will work
                        new FilePermission("<<ALL FILES>>", "read"),
                        // Required for the TimeoutUtil.adjust call when determining how long DEPLOYMENTB should poll
                        new PropertyPermission("ts.timeout.factor", "read"),
                        // Required for the client to connect
                        new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")
                ), "permissions.xml");
    }

    /**
     * 1. Start the container
     * 2. Deploy the applications
     * 3. Stop the container
     * 4. Restart the container. If non-graceful mode is working, the container should start successfully.
     */
    @Test
    public void testNonGracefulDeployment() {
        try {
            containerController.start(CONTAINER);

            deployer.deploy(DEPLOYMENTA);
            deployer.deploy(DEPLOYMENTB);

            containerController.stop(CONTAINER);
            containerController.start(CONTAINER);
        } finally {
            executeCleanup(() -> deployer.undeploy(DEPLOYMENTA));
            executeCleanup(() -> deployer.undeploy(DEPLOYMENTB));
            executeCleanup(() -> containerController.stop(CONTAINER));
        }
    }

    private void executeCleanup(Runnable func) {
        try {
            func.run();
        } catch (Exception e) {
            logger.trace("Exception during container cleanup and shutdown", e);
        }
    }
}
