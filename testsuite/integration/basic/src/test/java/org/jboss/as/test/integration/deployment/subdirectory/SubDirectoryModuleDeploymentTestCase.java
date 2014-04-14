package org.jboss.as.test.integration.deployment.subdirectory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;


/**
 * WFLY-29
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SubDirectoryModuleDeploymentTestCase {
    private static final Logger logger = Logger.getLogger(SubDirectoryModuleDeploymentTestCase.class);

    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "subdirectory.ear");

        final JavaArchive jarOne = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jarOne.addClass(MessageBean.class);
        ear.addAsModule(new ArchiveAsset(jarOne, ZipExporter.class), "subdir/ejb/ejb.jar");

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war");
        war.addClass(MessageServlet.class);
        ear.addAsModule(new ArchiveAsset(war, ZipExporter.class), "subdir/web/web.war");

        logger.info(ear.toString(true));
        return ear;
    }

    @ArquillianResource
    protected URL url;

    @Test
    public void testModulesInSubDeployments() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "message");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            Assert.assertEquals("Hello World", result);

        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }

    }

}
