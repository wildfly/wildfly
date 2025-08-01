/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.jwt.parallel;

import static org.wildfly.security.pem.Pem.generatePemPublicKey;
import static org.wildfly.test.integration.microprofile.jwt.BaseJWTCase.testAuthorizationRequired;
import static org.wildfly.test.integration.microprofile.jwt.BaseJWTCase.testJWTCall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.bytes.ByteStringBuilder;
import org.wildfly.test.integration.microprofile.jwt.App;
import org.wildfly.test.integration.microprofile.jwt.BaseJWTCase;
import org.wildfly.test.integration.microprofile.jwt.SampleEndPoint;

/*
 * Test case to test that if two web applications are deployed side by side each
 * with their own JWT configuration they will operate independently.
 *
 * This will be demonstrated by using different keys for each deployment.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JWTParallelDeploymentTestCase {


    private static final String DEPLOYMENT_ONE = "DeploymentOne.war";
    private static final String DEPLOYMENT_TWO = "DeploymentTwo.war";

    private static final KeyPair identity_one;
    private static final KeyPair identity_two;

    static {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            identity_one = keyPairGenerator.generateKeyPair();
            identity_two = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalSaslStateException("Unable to initialise key pairs.", e);
        }
    }

    private static final String MP_CONFIG = """
            mp.jwt.verify.publickey=%s
            mp.jwt.verify.issuer=quickstart-jwt-issuer
            """;

    @Deployment(name = DEPLOYMENT_ONE, testable = false)
    public static Archive<?> deploymentOne() {
        Archive<?> archive = deployment(DEPLOYMENT_ONE, identity_one.getPublic());
        System.out.println(archive.toString(true));
        return archive;
    }

    @Deployment(name = DEPLOYMENT_TWO, testable = false)
    public static Archive<?> deploymentTwo() {
        return deployment(DEPLOYMENT_TWO, identity_two.getPublic());
    }

    private static Archive<?> deployment(final String deploymentName, final PublicKey publicKey) {
        String microprofileConfig = String.format(MP_CONFIG, toPem(publicKey));
        System.out.println(microprofileConfig);

        return ShrinkWrap.create(WebArchive.class, deploymentName)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addClasses(App.class, SampleEndPoint.class)
                .addAsWebInfResource(BaseJWTCase.class.getPackage(), "web.xml", "web.xml")
                .addAsManifestResource(new StringAsset(microprofileConfig), "microprofile-config.properties");
    }

    private static String toPem(final PublicKey publicKey) {
        ByteStringBuilder byteStringBuilder = new ByteStringBuilder();
        generatePemPublicKey(byteStringBuilder, publicKey);

        StringReader sr = new StringReader(new String(byteStringBuilder.toArray(), StandardCharsets.UTF_8));
        BufferedReader br = new BufferedReader(sr);
        try {
            StringBuilder sb = new StringBuilder(br.readLine());
            String line;
            while ( (line = br.readLine()) != null) {
                sb.append("\\").append(System.lineSeparator());
                sb.append(line);
            }

            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to convert key", e);
        }
    }

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_ONE)
    private URL deploymentOneURL;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_TWO)
    private URL deploymentTwoURL;

    @Test
    public void testDeploymentOneRequiresAuthorization() throws Exception {
        testAuthorizationRequired(deploymentOneURL);
    }

    @Test
    public void testDeploymentTwoRequiresAuthorization() throws Exception {
        testAuthorizationRequired(deploymentTwoURL);
    }

    @Test
    public void testDeploymentOneIdentityOne() throws Exception {
        testJWTCall(identity_one::getPrivate, deploymentOneURL, true);
    }

    @Test
    public void testDeploymentOneIdentityTwo() throws Exception {
        testJWTCall(identity_two::getPrivate, deploymentOneURL, false);
    }

    @Test
    public void testDeploymentTwoIdentityOne() throws Exception {
        testJWTCall(identity_one::getPrivate, deploymentTwoURL, false);
    }

    @Test
    public void testDeploymentTwoIdentityTwo() throws Exception {
        testJWTCall(identity_two::getPrivate, deploymentTwoURL, true);
    }

}
