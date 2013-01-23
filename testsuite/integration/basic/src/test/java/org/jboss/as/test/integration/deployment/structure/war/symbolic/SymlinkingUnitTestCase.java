package org.jboss.as.test.integration.deployment.structure.war.symbolic;

import junit.framework.Assert;
import org.apache.commons.lang.SystemUtils;
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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;


/**
 * Unit test to make sure that the symbolic linking feature on the web subsystem is enabled properly.
 * Corresponding JIRA: AS7-3414.
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
    public static void beforeClass() throws IOException {
        logger.infof("beforeClass() call.");
        // We are going to check whether or not the war deployment actually exists.
        Assert.assertTrue(checkForDeployment());
        // Now create the symlink.
        createSymlink();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        logger.infof("afterClass() call.");
        // Delete the symlink
        symbolic.delete();
    }

    @After
    public void tearDown() throws IOException {
        logger.infof("In the tearDown() call.");
        ModelNode undefineOperation = Util.getUndefineAttributeOperation(PathAddress.pathAddress(getAddress()),
                                                                                "symbolic-linking-enabled");

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
        steps.add(undefineOperation);

        logger.infof("Composite operation: %s", compositeOperation.toString());
        ModelNode compositeResult = controllerClient.execute(compositeOperation);

        Assert.assertEquals(ModelDescriptionConstants.SUCCESS,
                                   compositeResult.get(ModelDescriptionConstants.OUTCOME).asString());

    }

    @Test
    public void testEnabled() {
        logger.infof("Testing enabled bit");
        // By default we should not be able to browse to the symlinked page.
        Assert.assertTrue(getURLcode("symbolic") == 404);
        try {
            setup(true);
        } catch (IOException e) {
            logger.fatalf("IOException caught while setting up in testEnabled()");
        }
        // First make sure that we can browse to index.html. This should work in the enabled or disabled version.
        Assert.assertTrue(getURLcode("index") == 200);
        // Now with symbolic.html.
        Assert.assertTrue(getURLcode("symbolic") == 200);
    }

    @Test
    public void testDisabled() {
        logger.infof("Testing disabled bit.");
        // By default we should not be able to browse to the symlinked page.
        Assert.assertTrue(getURLcode("symbolic") == 404);
        try {
            setup(false);
        } catch (IOException e) {
            logger.fatalf("IOException caught while setting up in testDisabled()");
        }
        // First make sure that we can browse to index.html. This should work in the enabled or disabled version.
        Assert.assertTrue(getURLcode("index") == 200);
        // Now with symbolic.html.
        Assert.assertTrue(getURLcode("symbolic") == 404);
    }

    private void setup(boolean symlinkingEnabled) throws IOException {
        logger.infof("Entered the setup call within %s & the boolean parameter is %s",
                            this.getClass().getName(), symlinkingEnabled);
        ModelNode writeOperation = Util.getWriteAttributeOperation(PathAddress.pathAddress(getAddress())
                                                                              , "symbolic-linking-enabled", symlinkingEnabled);
        // Define the content.
        ModelNode content = new ModelNode();
        content.get(0).get(ModelDescriptionConstants.ARCHIVE).set(false);
        content.get(0).get(ModelDescriptionConstants.PATH).set(warDeployment.getAbsolutePath());

        // Add operation.
        ModelNode addOperation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, WAR_NAME));
        addOperation.get(ModelDescriptionConstants.CONTENT).set(content);
        addOperation.get(ModelDescriptionConstants.ENABLE).set(true);

        // Deployment operation.
        ModelNode deploymentOperation = Util.getEmptyOperation(ModelDescriptionConstants.DEPLOY, new ModelNode().add(ModelDescriptionConstants.DEPLOYMENT, WAR_NAME));
        deploymentOperation.get(ModelDescriptionConstants.CONTENT).set(content);
        deploymentOperation.get(ModelDescriptionConstants.ENABLE).set(true);

        // Composite operation.
        ModelNode compositeOperation = Util.getEmptyOperation(ModelDescriptionConstants.COMPOSITE, new ModelNode());
        ModelNode steps = compositeOperation.get(ModelDescriptionConstants.STEPS);
        steps.add(writeOperation);
        steps.add(addOperation);
        steps.add(deploymentOperation);

        logger.infof("Composite operation: %s", compositeOperation.toString());
        ModelNode compositeResult = controllerClient.execute(compositeOperation);

        Assert.assertEquals(ModelDescriptionConstants.SUCCESS,
                                   compositeResult.get(ModelDescriptionConstants.OUTCOME).asString());
    }

    private int getURLcode(String htmlPage) {
        int code = 0;
        try {
            String url = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/explodedDeployment/"
                                 + htmlPage + ".html";
            logger.infof("%s is the built URL.", url);
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            code = urlConnection.getResponseCode();
            logger.infof("Received response code of: " + code);
        } catch (Exception e) {
            logger.infof("Exception of type: %s caught", e.getClass(), e);
        }
        return code;
    }

    private ModelNode getAddress() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "web");
        address.protect();
        logger.infof("Created the address ModelNode with subsystem=web.");
        return address;
    }

    private static boolean checkForDeployment() {
        String warLocation = getWarLocation();
        warDeployment = new File(warLocation);
        logger.infof("Checking to see if exploded deployment exists at path: " + warDeployment.getAbsolutePath());
        return warDeployment.exists();
    }

    private static String getWarLocation() {
        // Hack of a method where we hop through the filesystem in order to try and locate the war file.
        // Currently set up like this for want of a more efficient approach.
        String preSubString = null, postSubString = null;

        try {
            String path = SymlinkingUnitTestCase.class.getResource(WAR_NAME).toURI().toString();

            // From running this a few times, we know that the String looks like:
            // file:/{$jboss.testsuite.basic}/target/test-classes/org/jboss/as/test/integration/deployment/structure
            // /war/symbolic/explodedDeployment.war

            // Required:
            // {$jboss.testsuite.basic}/src/test/resources/deployment/structure/war/symbolic/explodedDeployment.war

            preSubString = path.substring(path.indexOf("/"), path.indexOf("target"));
            logger.debugf("Pre-substring: ", preSubString);

            postSubString = path.substring(path.indexOf("deployment"));
            logger.debugf("Post-substring" + postSubString);

        } catch (URISyntaxException e) {
            logger.fatalf("Error converting URL to URI.", e);
        }
        return preSubString + "src/test/resources/" + postSubString;
    }

    private static void createSymlink() {
        // TODO: navssurtani: once AS7 is on a minimum of Java 7 then we can change the approach used to create the symlink.
        File index = new File(warDeployment, "index.html");
        logger.infof("Path to index file is: " + index.getAbsolutePath());
        Assert.assertTrue(index.exists());
        // Now set up the information to create the symlink.
        String toExecute;

        if (SystemUtils.IS_OS_WINDOWS) {
            logger.infof("Windows based OS detected.");
            toExecute = "mklink \\D " + index.getAbsolutePath() + " " + warDeployment.getAbsolutePath()
                                + "\\symbolic.html";
        } else {
            logger.infof("UNIX based OS detected.");
            toExecute = "ln -s " + index.getAbsolutePath() + " " + warDeployment.getAbsolutePath() + "/symbolic.html";
        }

        logger.infof("String to be executed is: " + toExecute);

        try {
            Runtime.getRuntime().exec(toExecute);
            symbolic = new File(warDeployment.getAbsolutePath(), "symbolic.html");
        } catch (IOException e) {
            logger.fatalf("Caught IOException while trying to create the symbolic link using the command: " +
                                  toExecute, e);
        }

        logger.infof("Absolute path of symbolic file is located at %s. Checking to see if the file really exists.",
                            symbolic.getAbsolutePath());
        Assert.assertTrue(symbolic.exists());
    }
}
