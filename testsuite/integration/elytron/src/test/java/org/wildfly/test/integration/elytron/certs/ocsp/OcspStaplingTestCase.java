package org.wildfly.test.integration.elytron.certs.ocsp;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.security.Security;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.version.Stability;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.test.integration.elytron.util.WelcomeContent;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

/**
 * Tests to check OCSP Stapling feature using XiPKI OCSP responder.
 *
 * @author Prarthona Paul <prpaul@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({OcspStaplingTestCase.PreviewStabilitySetupTask.class, OcspTestBase.OcspStaplingServerSetup.class, WelcomeContent.SetupTask.class})
public class OcspStaplingTestCase extends OcspTestBase {

    private static SSLContext softFailClientContext;
    private static SSLContext hardFailClientContext;

    public OcspStaplingTestCase() {
        super(Stability.PREVIEW);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        softFailClientContext = createSSLContextForOcspStapling(ladybirdKeyStore, trustStore, PASSWORD, true);
        hardFailClientContext = createSSLContextForOcspStapling(ladybirdKeyStore, trustStore, PASSWORD, false);
    }

    @AfterClass
    public static void cleanup() {
        Security.setProperty("ocsp.enable", "");
        System.clearProperty("jdk.tls.client.enableStatusRequestExtension");
    }

    @Test
    public void testOcspStaplingGood() throws Throwable {
        performConnectionOcspStaplingTest(softFailClientContext, true);
        performConnectionOcspStaplingTest(hardFailClientContext, true);
    }

    @Test
    public void testOcspStaplingRevoked() throws Throwable {
        setServerKeyStore("serverKeyStoreRevoked");
        performConnectionOcspStaplingTest(softFailClientContext, false);
        performConnectionOcspStaplingTest(hardFailClientContext, false);
    }

    @Test
    public void testOcspStaplingUnknownSoftFail() throws Throwable {
        setServerKeyStore("serverKeyStoreUnknown");
        performConnectionOcspStaplingTest(softFailClientContext, true);
    }

    @Test
    public void testOcspStaplingUnknownHardFail() throws Throwable {
        setServerKeyStore("serverKeyStoreUnknown");
        performConnectionOcspStaplingTest(hardFailClientContext, false);
    }

    /**
     * This deployment is required just so OcspStaplingServerSetup task is executed.
     */
    @Deployment
    protected static WebArchive createDeployment() {
        return createDeployment("dummy");
    }

    @ArquillianResource
    protected URL url;

    protected static WebArchive createDeployment(final String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war");
    }

    public static class PreviewStabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Preview {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Write a system property so the model gets stored with a lower stability level.
            // This is to make sure we can reload back to the higher level from the snapshot
            OcspTestBase.addSystemProperty(managementClient, OcspStaplingTestCase.class);
        }
    }

}
