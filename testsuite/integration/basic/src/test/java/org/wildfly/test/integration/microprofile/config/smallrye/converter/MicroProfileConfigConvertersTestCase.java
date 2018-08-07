package org.wildfly.test.integration.microprofile.config.smallrye.converter;

import static org.wildfly.test.integration.microprofile.config.smallrye.HttpUtils.getContent;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.config.smallrye.AssertUtils;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SetupTask.class)
public class MicroProfileConfigConvertersTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileConfigConvertersTestCase.war")
                .addClasses(TestApplication.class, TestApplication.Resource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(TestApplication.class.getPackage(), "jboss-deployment-structure.xml",
                        "jboss-deployment-structure.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testConverterPriority() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "custom-converter/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);
            AssertUtils.assertTextContainsProperty(text, "int_converted_to_102_by_priority_of_custom_converter", "102");
            // TODO - enable this when https://issues.jboss.org/browse/WFWIP-60 is resolved
            //AssertUtils.assertTextContainsProperty(text, "string_converted_by_priority_of_custom_converter", "Property converted by HighPriorityStringConverter1");
        }
    }
}