package org.jboss.as.test.integration.ws.basic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;

//import org.jboss.shrinkwrap.api.spec.JavaArchive;


@RunWith(Arquillian.class)
@RunAsClient
public class InstanceCountEndpointTestcase {


    public static final String ARCHIVE_NAME = "instanceCountWebservice";

    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addClasses(InstanceCountEndpoint.class, InstanceCountEndpointIface.class);
        return war;
    }

    @Test
    public void testforcheckingInstances() throws Exception {
        final QName serviceName = new QName("http://jbossws.org/basic", "SimpleService");
        final URL wsdlURL = new URL(baseUrl, "/instanceCountWebservice/SimpleService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        InstanceCountEndpointIface proxy = service.getPort(InstanceCountEndpointIface.class);
        Assert.assertEquals("OK", proxy.test("test"));
        int result = proxy.getInstanceCount();
        Assert.assertEquals(1, result);

    }
}
