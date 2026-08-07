package org.jboss.as.test.integration.ejb.noremote;

import jakarta.ejb.EJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This class tests that when the "remote" resource of the ejb3 subsystem is removed, we may still make
 * local invocations on both @Remote and @Local interfaces.
 */
@RunWith(Arquillian.class)
@ServerSetup(LocalInvocationWithRemoteResourceRemovedTestCase.LocalInvocationWithRemoteResourceRemovedTestCaseSetup.class)
public class LocalInvocationWithRemoteResourceRemovedTestCase {

    private static final Logger logger = Logger.getLogger(LocalInvocationWithRemoteResourceRemovedTestCase.class);

    private static final String APP_NAME = "";
    private static final String MODULE_NAME = "local-invocation-with-remote-resource-removed-test";
    private static final String DISTINCT_NAME = "";

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(LocalInvocationWithRemoteResourceRemovedTestCase.class.getPackage());
        return ejbJar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    /**
     * Test that local invocation on a local view of a stateless bean is allowed.
     *
     * NOTE: this is an EJB client invocation from a Arquillian test case run in-container and
     * is not equivalent to an EJB client invocation from a deployment.
     *
     * @throws Exception
     */
    @Test
    public void testLocalInvocationOnLocalViewSLSB() throws Exception {
        LocalEcho localEcho = lookup(LocalEchoBean.class.getSimpleName(), LocalEcho.class);
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            logger.info("Local invocation on bean with a @Local interface works!");
        } catch (EJBException nsee) {
            Assert.fail("Local invocation on an @Local view " + LocalEcho.class.getName() + " was expected to pass");
        }
    }

    /**
     * Test that local invocation on a remote view of a stateless bean is allowed.
     *
     * NOTE: this is an EJB client invocation from a Arquillian test case run in-container and
     * is not equivalent to an EJB client invocation from a deployment.
     *
     * @throws Exception
     */
    @Test
    public void testLocalInvocationOnRemoteViewSLSB() throws Exception {
        RemoteEcho remoteEcho = lookup(RemoteEchoBean.class.getSimpleName(), RemoteEcho.class);
        final String message = "Silence!";
        try {
            final String echo = remoteEcho.echo(message);
            logger.info("Local invocation on bean with a @Remote interface works!");
        } catch (EJBException nsee) {
            Assert.fail("Remote invocation on a @Remote view " + RemoteEcho.class.getName() + " was expected to pass");
        }
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + MODULE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    // the snapshot restore setup task will return configuration to its original state
    static class LocalInvocationWithRemoteResourceRemovedTestCaseSetup extends SnapshotRestoreSetupTask {
        @Override
        public void doSetup(final ManagementClient managementClient, final String containerId) throws Exception {
             // remove the service=remote resource
             EJBManagementUtil.removeRemoteResource(managementClient);
             logger.info("The ejb subsystem remote resource has been removed");
        }
    }
}
