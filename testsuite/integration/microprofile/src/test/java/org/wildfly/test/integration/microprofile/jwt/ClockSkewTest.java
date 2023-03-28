/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.wildfly.test.integration.microprofile.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.io.File;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.PrivateKey;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.wildfly.common.iteration.CodePointIterator;
import org.wildfly.security.pem.Pem;
import org.wildfly.test.integration.microprofile.jwt.norealm.App;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Tests for mp.jwt.verify.clock.skew property introduced in MP JWT 2.1
 */
@RunAsClient
@RunWith(Arquillian.class)
public class ClockSkewTest {

    @ArquillianResource
    private URL baseURL;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class, "ClockSkewTest.war")
                .addClasses(App.class, SampleEndPoint.class, BaseCase.class)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addAsManifestResource(new FileAsset(new File("src/test/resources/jwt/microprofile-config-with-clock-skew.properties")),
                        "microprofile-config.properties")
                .addAsManifestResource(new FileAsset(new File("src/test/resources/jwt/public.pem")), "public.pem");
    }

    @Test
    public void testClockSkewTokenNotExpired() throws Exception {
        String token = createJwt();
        Thread.sleep(3000); // exp is 3 seconds but request will be accepted because the clock skew is set to 2 seconds
        callEchoAndExpectStatus(token, HttpURLConnection.HTTP_OK);
    }

    @Test
    public void testClockSkewExpired() throws Exception {
        String token = createJwt();
        Thread.sleep(5000); // exp + clock skew is 5 seconds, so wait 5 seconds and test that request will be unauthorized
        callEchoAndExpectStatus(token, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    private void callEchoAndExpectStatus(String token, int status) {
        Response response = callEcho(token);
        Assert.assertEquals(status, response.getStatus());
    }

    private Response callEcho(String token) {
        String uri = baseURL.toExternalForm() + "/rest/Sample/subscription";
        WebTarget echoEndpointTarget = ClientBuilder.newClient().target(uri);
        return echoEndpointTarget.request(TEXT_PLAIN).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
    }

    public static int currentTimeInSecs() {
        long currentTimeMS = System.currentTimeMillis();
        return (int) (currentTimeMS / 1000);
    }

    public String createJwt() throws Exception {

        JsonObjectBuilder claimsBuilder = Json.createObjectBuilder()
                .add("sub", "testSubject")
                .add("iss", "quickstart-jwt-issuer")
                .add("aud", "testAud")
                .add("groups", Json.createArrayBuilder().add("Subscriber").build())
                .add("iat", ((System.currentTimeMillis() / 1000)))
                .add("exp", ((System.currentTimeMillis() / 1000) + 3)); // exp is 3 seconds from now

        JWSObject jwsObject = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("jwt")).build(), new Payload(claimsBuilder.build().toString()));

        String pemContent = Files.readString(Path.of("src/test/resources/jwt/private.pem"));
        PrivateKey privateKey = Pem.parsePemContent(CodePointIterator.ofString(pemContent)).next().tryCast(PrivateKey.class);
        jwsObject.sign(new RSASSASigner(privateKey));

        return jwsObject.serialize();
    }
}
