package org.jboss.as.test.integration.ejb.iiop.naming;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * Tests that corba name lookups work from inside the AS itself
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class IIOPNamingInContainerTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(name="test")
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addPackage(IIOPNamingInContainerTestCase.class.getPackage())
                .addAsManifestResource(IIOPNamingInContainerTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(IIOPNamingInContainerTestCase.class.getPackage(), "permissions.xml", "permissions.xml");
    }

    @Test
    @OperateOnDeployment("test")
    public void testIIOPNamingInvocation() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:" + managementClient.getMgmtAddress() + ":3528#IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    @Test
    @OperateOnDeployment("test")
    public void testStatefulIIOPNamingInvocation() throws NamingException, RemoteException, RemoveException {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:" + managementClient.getMgmtAddress() + ":3528#IIOPStatefulNamingBean");
        final IIOPStatefulNamingHome object = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPStatefulNamingHome.class);
        final IIOPStatefulRemote result = object.create(10);
        Assert.assertEquals(11, result.increment());
        Assert.assertEquals(12, result.increment());
        result.remove();
        try {
            result.increment();
            Assert.fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {

        }
    }
}
