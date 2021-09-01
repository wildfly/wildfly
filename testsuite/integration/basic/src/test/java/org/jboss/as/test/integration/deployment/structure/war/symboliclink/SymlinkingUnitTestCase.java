package org.jboss.as.test.integration.deployment.structure.war.symboliclink;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.junit.Assume;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Unit test to make sure that the symbolic linking feature on the web subsystem is enabled properly.
 * Corresponding JIRA: AS7-3414.
 *
 * @author navssurtani
 */

@RunWith(Arquillian.class)
@RunAsClient
public class SymlinkingUnitTestCase {

    private static final Logger logger = Logger.getLogger(SymlinkingUnitTestCase.class);
    private static final String WAR_NAME = "explodedDeployment.war";

    private static File warDeployment = null;
    private static File symbolic = null;
    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();


    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);
        // We are going to check whether or not the war deployment actually exists.
        Assert.assertTrue(checkForDeployment());
        // Now create the symlink.
        createSymlink();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
         // skip windows;
        } else {
            // Delete the symlink
            symbolic.delete();
            controllerClient.close();
            controllerClient = null;
        }
    }

    @After
    public void tearDown() throws IOException {

        ModelNode removeSystemProperty = Util.createRemoveOperation(PathAddress.pathAddress(systemPropertyAddress()));

        // Define the content.
        ModelNode content = new ModelNode();
        content.get(0).get(ModelDescriptionConstants.ARCHIVE).set(false);
        content.get(0).get(ModelDescriptionConstants.PATH).set(warDeployment.getAbsolutePath());

        // Undeploy operation.
        ModelNode undeployOperation = Util.getEmptyOperation(ModelDescriptionConstants.UNDEPLOY, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, WAR_NAME));
        undeployOperation.get(ModelDescriptionConstants.CONTENT).set(ModelDescriptionConstants.CONTENT);
        undeployOperation.get(ModelDescriptionConstants.ENABLE).set(true);

        // Remove operation.
        ModelNode removeOperation = Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, WAR_NAME));
        removeOperation.get(ModelDescriptionConstants.CONTENT).set(ModelDescriptionConstants.CONTENT);
        removeOperation.get(ModelDescriptionConstants.ENABLE).set(true);

        // Composite operation.
        ModelNode compositeOperation = Util.getEmptyOperation(ModelDescriptionConstants.COMPOSITE, new ModelNode());
        ModelNode steps = compositeOperation.get(ModelDescriptionConstants.STEPS);
        steps.add(undeployOperation);
        steps.add(removeOperation);
        steps.add(removeSystemProperty);

        ModelNode compositeResult = controllerClient.execute(compositeOperation);

        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, compositeResult.get(ModelDescriptionConstants.OUTCOME).asString());

    }

    @Test
    public void testEnabled() throws IOException {
        // By default we should not be able to browse to the symlinked page.
        Assert.assertTrue(getURLcode("symbolic") == 404);
        setup(true);
        // First make sure that we can browse to index.html. This should work in the enabled or disabled version.
        Assert.assertTrue(getURLcode("index") == 200);
        // Now with symbolic.html.
        Assert.assertTrue(getURLcode("symbolic") == 200);
    }

    @Test
    public void testDisabled() throws IOException {
        // By default we should not be able to browse to the symlinked page.
        Assert.assertTrue(getURLcode("symbolic") == 404);
        setup(false);
        // First make sure that we can browse to index.html. This should work in the enabled or disabled version.
        Assert.assertTrue(getURLcode("index") == 200);
        // Now with symbolic.html.
        Assert.assertTrue(getURLcode("symbolic") == 404);
    }

    private void setup(boolean symlinkingEnabled) throws IOException {
        ModelNode addSysProperty = Util.createAddOperation(PathAddress.pathAddress(systemPropertyAddress()));
        addSysProperty.get(ModelDescriptionConstants.VALUE).set(Boolean.toString(symlinkingEnabled));
        ModelNode result = controllerClient.execute(addSysProperty);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

        // Define the content.
        ModelNode content = new ModelNode();
        content.get(0).get(ModelDescriptionConstants.ARCHIVE).set(false);
        content.get(0).get(ModelDescriptionConstants.PATH).set(warDeployment.getAbsolutePath());

        // Add operation.
        ModelNode addOperation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, WAR_NAME));
        addOperation.get(ModelDescriptionConstants.CONTENT).set(content);
        addOperation.get(ModelDescriptionConstants.ENABLE).set(true);
        result = controllerClient.execute(addOperation);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());

        // Deployment operation.
        ModelNode deploymentOperation = Util.getEmptyOperation(ModelDescriptionConstants.DEPLOY, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, WAR_NAME));
        deploymentOperation.get(ModelDescriptionConstants.CONTENT).set(content);
        deploymentOperation.get(ModelDescriptionConstants.ENABLE).set(true);
        result = controllerClient.execute(deploymentOperation);
        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
    }

    private int getURLcode(String htmlPage) {
        int code = 0;
        try {
            String url = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/explodedDeployment/"
                    + htmlPage + ".html";
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            code = urlConnection.getResponseCode();
        } catch (Exception e) {
            logger.errorf(e, "Exception of type: %s caught", e.getClass());
            Assert.fail("Failed to get the URL code: " + e.getLocalizedMessage());
        }
        return code;
    }

    private ModelNode systemPropertyAddress() {
        ModelNode address = new ModelNode();
        address.add("system-property", "symbolic-linking-enabled");
        address.protect();
        return address;
    }

    private static boolean checkForDeployment() {
        String warLocation = getWarLocation();
        warDeployment = new File(warLocation);
        return warDeployment.exists();
    }

    private static String getWarLocation() {
        return SymlinkingUnitTestCase.class.getResource(WAR_NAME).getPath();
    }

    private static void createSymlink() throws IOException, InterruptedException {
        // TODO: navssurtani: once AS7 is on a minimum of Java 7 then we can change the approach used to create the symlink.
        File index = new File(warDeployment, "index.html");
        Assert.assertTrue(index.exists());
        // Now set up the information to create the symlink.
        String toExecute;

        if (SystemUtils.IS_OS_WINDOWS) {
            toExecute = "mklink \\D " + index.getAbsolutePath() + " " + warDeployment.getAbsolutePath()
                    + "\\symbolic.html";
        } else {
            toExecute = "ln -s " + index.getAbsolutePath() + " " + warDeployment.getAbsolutePath() + "/symbolic.html";
        }

        logger.debugf("String to be executed is: %s", toExecute);

        Runtime.getRuntime().exec(toExecute).waitFor();
        symbolic = new File(warDeployment.getAbsolutePath(), "symbolic.html");


        logger.debugf("Absolute path of symbolic file is located at %s. Checking to see if the file really exists.",
                symbolic.getAbsolutePath());
        Assert.assertTrue(symbolic.exists());
    }
}
