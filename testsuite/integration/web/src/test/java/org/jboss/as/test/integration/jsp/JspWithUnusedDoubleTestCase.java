package org.jboss.as.test.integration.jsp;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class JspWithUnusedDoubleTestCase {
    private static final String DEPLOYMENT = "jsp-with-unused-double.war";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT);
        war.add(new UrlAsset(JspWithUnusedDoubleTestCase.class.getResource("jsp-with-unused-double.jsp")), "jsp-with-unused-double.jsp");
        return war;
    }


    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testUnusedDoubleInJsp(@ArquillianResource URL url) throws TimeoutException, ExecutionException, IOException {
        String response = HttpRequest.get(url + "jsp-with-unused-double.jsp", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Jsp doesn't contain valid output", "1.0", response.trim());
    }
}
