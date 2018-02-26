package org.jboss.as.test.integration.ejb.remote.http;

import java.net.URL;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class HttpInvocationInContainerTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "http-test.war")
                .addPackage(HttpInvocationInContainerTestCase.class.getPackage());
    }

    @Test
    public void invokeEjb() throws NamingException {
        Hashtable table = new Hashtable();
        table.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        table.put(Context.PROVIDER_URL, "http://" + url.getHost() + ":" + url.getPort() + "/wildfly-services");
        table.put(Context.SECURITY_PRINCIPAL, "user1");
        table.put(Context.SECURITY_CREDENTIALS, "password1");
        InitialContext ic = new InitialContext(table);
        EchoRemote echo = (EchoRemote) ic.lookup("http-test/EchoBean!org.jboss.as.test.integration.ejb.remote.http.EchoRemote");
        Assert.assertEquals("hello", echo.echo("hello"));
    }
}
