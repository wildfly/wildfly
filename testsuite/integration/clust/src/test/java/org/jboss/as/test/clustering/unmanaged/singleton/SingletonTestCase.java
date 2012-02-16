package org.jboss.as.test.clustering.unmanaged.singleton;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.unmanaged.singleton.service.MyService;
import org.jboss.as.test.clustering.unmanaged.singleton.service.MyServiceContextListener;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class SingletonTestCase {

    /** Constants **/
    public static final long GRACE_TIME_TO_MEMBERSHIP_CHANGE = 5000;
    public static final String CONTAINER1 = "clustering-udp-0-unmanaged";
    public static final String CONTAINER2 = "clustering-udp-1-unmanaged";
    public static final String DEPLOYMENT1 = "deployment-0-unmanaged";
    public static final String DEPLOYMENT2 = "deployment-1-unmanaged";

    @ArquillianResource
    ContainerController controller;
    @ArquillianResource
    Deployer deployer;

    @BeforeClass
    public static void printSysProps() {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }

    @Deployment(name = DEPLOYMENT1, managed = false, testable = false)
    @TargetsContainer(CONTAINER1)
    public static Archive<?> deployment0() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "singleton.war");
        war.addPackage(MyService.class.getPackage());
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.singleton, org.jboss.as.server\n"));
        System.out.println(war.toString(true));
        return war;
    }

    @Deployment(name = DEPLOYMENT2, managed = false, testable = false)
    @TargetsContainer(CONTAINER2)
    public static Archive<?> deployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "singleton.war");
        war.addPackage(MyService.class.getPackage());
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.singleton, org.jboss.as.server\n"));
        System.out.println(war.toString(true));
        return war;
    }

    @Test
    @InSequence(1)
    /* @OperateOnDeployment(DEPLOYMENT1) -- See http://community.jboss.org/thread/176096 */
    public void test(/*@ArquillianResource(SimpleServlet.class) URL baseURL*/) throws IOException, InterruptedException {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER1);
        deployer.deploy(DEPLOYMENT1);

        DefaultHttpClient client = new DefaultHttpClient();

        // ARQ-674 Ouch, second hardcoded URL will need fixing. ARQ doesnt support @OperateOnDeployment on 2 containers.
        String url1 = "http://127.0.0.1:8080/singleton/service"; /* baseURL.toString() + "simple"; */
        String url2 = "http://127.0.0.1:8180/singleton/service";

        try {
            HttpResponse response = client.execute(new HttpGet(url1));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("node-udp-0", response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.start(CONTAINER2);
            deployer.deploy(DEPLOYMENT2);


            response = client.execute(new HttpGet(url1));
            long startTime = System.currentTimeMillis();
            while(response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK && startTime + GRACE_TIME_TO_MEMBERSHIP_CHANGE > System.currentTimeMillis()) {
                response = client.execute(new HttpGet(url1));
            }
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            response = tryGet(client, url2);;
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.stop(CONTAINER2);

            response = tryGet(client, url1);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("node-udp-0", response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.start(CONTAINER2);

            response = tryGet(client, url1);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.stop(CONTAINER1);

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("node-udp-1", response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            controller.start(CONTAINER1);

            response = tryGet(client, url1);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();

            response = tryGet(client, url2);
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(MyServiceContextListener.PREFERRED_NODE, response.getFirstHeader("node").getValue());
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();

            deployer.undeploy(DEPLOYMENT1);
            controller.stop(CONTAINER1);
            deployer.undeploy(DEPLOYMENT2);
            controller.stop(CONTAINER2);
        }
    }

    private HttpResponse tryGet(final DefaultHttpClient client, final String url1) throws IOException {
        final long startTime;
        HttpResponse response = client.execute(new HttpGet(url1));
        startTime = System.currentTimeMillis();
        while(response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK && startTime + GRACE_TIME_TO_MEMBERSHIP_CHANGE > System.currentTimeMillis()) {
            response = client.execute(new HttpGet(url1));
        }
        return response;
    }
}
