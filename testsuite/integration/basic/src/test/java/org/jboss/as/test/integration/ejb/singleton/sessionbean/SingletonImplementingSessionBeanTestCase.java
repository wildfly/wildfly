package org.jboss.as.test.integration.ejb.singleton.sessionbean;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

@RunWith(Arquillian.class)
public class SingletonImplementingSessionBeanTestCase {

    private static final PathAddress LOG_FILE_ADDRESS = PathAddress.pathAddress()
            .append(SUBSYSTEM, "logging")
            .append("log-file", "server.log");

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "singleton-session-bean.jar");
        jar.addPackage(SingletonImplementingSessionBeanTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller\n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    @RunAsClient()
    public void testWarningDuringDeployment() throws IOException {
        Assert.assertTrue("Log should contain warning that singleton bean can't implement SessionBean interface",
                retrieveServerLog().contains("WFLYEJB0515"));
    }

    private String retrieveServerLog() throws IOException {
        ModelNode op = Util.createEmptyOperation("read-log-file", LOG_FILE_ADDRESS);
        op.get("lines").set(30);
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        return result.get(RESULT).asList().stream().map(ModelNode::toString).collect(Collectors.joining("\n"));
    }
}
