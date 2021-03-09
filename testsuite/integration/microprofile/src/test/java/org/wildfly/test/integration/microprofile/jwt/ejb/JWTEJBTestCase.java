/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.jwt.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.microprofile.jwt.TokenUtil.generateJWT;

import java.io.File;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case for an Jakarta Enterprise Beans endpoint secured using the MP-JWT mechanism and invoking a
 * second Jakarta Enterprise Beans with role restrictions.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JWTEJBTestCase {

    private static final String DEPLOYMENT_NAME = JWTEJBTestCase.class.getSimpleName() + ".war";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addClasses(JWTEJBTestCase.class)
                .addClasses(App.class, BeanEndPoint.class, TargetBean.class)
                .addAsManifestResource(new FileAsset(
                        new File("src/test/resources/jwt/microprofile-config.properties")),
                        "microprofile-config.properties")
                .addAsManifestResource(
                        new FileAsset(new File("src/test/resources/jwt/public.pem")),
                        "public.pem");
    }

    private static final String KEY_LOCATION = "src/test/resources/jwt/private.pem";

    private static final String ROOT_PATH = "/rest/Sample/";
    private static final String SUBSCRIPTION = "subscription";

    private static final String DATE = "2017-09-15";
    private static final String ECHOER_GROUP = "Echoer";
    private static final String SUBSCRIBER_GROUP = "Subscriber";

    private static final String PRINCIPAL_NAME = "testUser";

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";

    private final CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    @ArquillianResource
    private URL deploymentUrl;

    @Test
    public void testAuthorized() throws Exception {
        String jwtToken = generateJWT(KEY_LOCATION, PRINCIPAL_NAME, DATE, ECHOER_GROUP, SUBSCRIBER_GROUP);

        HttpGet httpGet = new HttpGet(deploymentUrl.toString() + ROOT_PATH + SUBSCRIPTION);
        httpGet.addHeader(AUTHORIZATION, BEARER + " " + jwtToken);

        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("Successful call", 200, httpResponse.getStatusLine().getStatusCode());
        String body = EntityUtils.toString(httpResponse.getEntity());
        assertTrue("Call was authenticated", body.contains(PRINCIPAL_NAME));

        httpResponse.close();
    }

}
