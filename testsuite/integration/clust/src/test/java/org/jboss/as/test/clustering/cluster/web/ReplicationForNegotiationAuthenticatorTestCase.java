package org.jboss.as.test.clustering.cluster.web;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.ViewChangeListener;
import org.jboss.as.test.clustering.ViewChangeListenerBean;
import org.jboss.as.test.clustering.ViewChangeListenerServlet;
import org.jboss.as.test.clustering.single.web.SimpleServlet;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("BZ-949737")
public class ReplicationForNegotiationAuthenticatorTestCase extends ClusteredWebFailoverAbstractCase {

	
    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }
       
    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "distributable.war");
        war.addClass(SimpleServlet.class);
        // Take web.xml from the managed test.
        war.setWebXML(ClusteredWebSimpleTestCase.class.getPackage(), "web.xml");
        war.addClasses(ViewChangeListenerServlet.class, ViewChangeListener.class, ViewChangeListenerBean.class);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.msc, org.jboss.as.clustering.common, org.infinispan, org.jboss.as.server\n"));
        war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.jboss.security.negotiation"),"jboss-deployment-structure.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset("other", "org.jboss.security.negotiation.NegotiationAuthenticator"), "jboss-web.xml");
        return war;
    }
    
    @Test  
    @InSequence(0)
    public void testOneRequestSimpleFailover(
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource(SimpleServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, ExecutionException, URISyntaxException {

    	DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        String url1 = baseURL1.toString() + "simple";
        String url2 = baseURL2.toString() + "simple";

        try {
            
            HttpResponse response = client.execute(new HttpGet(url1));
            try {
                log.info("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals(1, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
            
            // Now check on the 2nd server
            
            response = client.execute(new HttpGet(url2));
            try {
                log.info("Requested " + url2 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", 2, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
           
            //and back on the 1st server
            response = client.execute(new HttpGet(url1));
            try {
                log.info("Requested " + url1 + ", got " + response.getFirstHeader("value").getValue() + ".");
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Assert.assertEquals("Session failed to replicate after container 1 was brough up.", 3, Integer.parseInt(response.getFirstHeader("value").getValue()));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
            
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

    }

}
