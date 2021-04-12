package org.jboss.as.test.integration.deployment.dependencies;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ArchiveDeployer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Tests inter deployment dependencies that are established via @EJB Injection
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbDependencyRestartTestCase {

    // Dummy deployment so arq will be able to inject a ManagementClient
    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar");
    }

    private static JavaArchive DEPENDEE = ShrinkWrap.create(JavaArchive.class, "dependee.jar")
            .addClasses(DependeeEjb.class, StringView.class);

    private static WebArchive DEPENDENT = ShrinkWrap.create(WebArchive.class, "dependent.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClasses(DependentInjectServlet.class, StringView.class);

    @ArquillianResource
    public ManagementClient managementClient;


    // We don't inject this via @ArquillianResource because ARQ can't fully control
    // DEPENDEE and DEPENDENT and things go haywire if we try. But we use ArchiveDeployer
    // because it's a convenient API for handling deploy/undeploy of Shrinkwrap archives
    private ArchiveDeployer deployer;

    @Before
    public void setup() {
        deployer = new ArchiveDeployer(managementClient);
    }

    @After
    public void cleanUp() {
        try {
            deployer.undeploy(DEPENDENT.getName());
        } catch (Exception e) {
            // Ignore
        }

        try {
            deployer.undeploy(DEPENDEE.getName());
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    public void testDeploymentDependenciesWithRestart() throws IOException, DeploymentException, TimeoutException, ExecutionException {

        deployer.deploy(DEPENDEE);
        deployer.deploy(DEPENDENT);

        Assert.assertEquals("hello", doGetRequest());

        ModelNode response = managementClient.getControllerClient().execute(Util.createEmptyOperation("redeploy", PathAddress.pathAddress("deployment", DEPENDEE.getName())));
        Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());

        Assert.assertEquals("hello", doGetRequest());
    }

    protected String doGetRequest() throws IOException, ExecutionException, TimeoutException {
        return HttpRequest.get(managementClient.getWebUri() + "/dependent/test", 10, TimeUnit.SECONDS);
    }

}
