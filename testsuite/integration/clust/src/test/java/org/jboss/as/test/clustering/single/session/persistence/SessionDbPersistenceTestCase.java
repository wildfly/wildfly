package org.jboss.as.test.clustering.single.session.persistence;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
@Ignore("https://issues.jboss.org/browse/WFLY-1655")
public class SessionDbPersistenceTestCase {

    @ArquillianResource
    protected ContainerController controller;
    @ArquillianResource
    protected Deployer deployer;

    private static final String DEPLOYMENT_NAME = "websession-db";
    protected static final String CONTAINER1 = "container-0";

    @Deployment(name = DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(CONTAINER1)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "websession-db.war");
        war.addClass(SimpleServlet.class);
        war.setWebXML(SessionDbPersistenceTestCase.class.getPackage(), "web.xml");
        war.addAsWebInfResource("WEB-INF/jboss-web.xml","jboss-web.xml");
        return war;
    }

    @Test
    @InSequence(-1)
    public void testSetup() {
        controller.start(CONTAINER1);
        deployer.deploy(DEPLOYMENT_NAME);
    }

    @Test
    public void testSessionDbPersistenceRedeploy(@ArquillianResource(SimpleServlet.class) URL baseURL)
            throws ClientProtocolException, IOException {

        DefaultHttpClient client = new DefaultHttpClient();
        String url = baseURL.toString() + "simple";
        try {
            HttpResponse response = client.execute(new HttpGet(url));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            deployer.undeploy(DEPLOYMENT_NAME);
            deployer.deploy(DEPLOYMENT_NAME);

            response = client.execute(new HttpGet(url));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Session persistence to DB was not successfull after redeploy.", 2,
                    Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

    @Test
    public void testSessionDbPersistenceRestart(@ArquillianResource(SimpleServlet.class) URL baseURL)
            throws ClientProtocolException, IOException {

        DefaultHttpClient client = new DefaultHttpClient();

        String url = baseURL.toString() + "simple";

        try {
            HttpResponse response = client.execute(new HttpGet(url));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();

            controller.stop(CONTAINER1);
            controller.start(CONTAINER1);

            response = client.execute(new HttpGet(url));
            Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Session persistence to DB was not successfull after restart.", 2,
                    Integer.parseInt(response.getFirstHeader("value").getValue()));
            response.getEntity().getContent().close();
        } finally {
            client.getConnectionManager().shutdown();
        }

    }

}
