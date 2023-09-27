/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.norealm;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.jwt.BaseCase;
import org.wildfly.test.integration.microprofile.jwt.SampleEndPoint;

/**
 * A smoke test case for a simple JWT deployment and invocation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JWTNoRealmTestCase extends BaseCase {

    private static final String DEPLOYMENT_NAME = JWTNoRealmTestCase.class.getSimpleName() + ".war";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addClasses(BaseCase.class, JWTNoRealmTestCase.class)
                .addClasses(App.class, SampleEndPoint.class)
                .addAsWebInfResource(new FileAsset(new File("src/test/resources/jwt/web.xml")), "web.xml")
                .addAsManifestResource(new FileAsset(new File("src/test/resources/jwt/microprofile-config.properties")), "microprofile-config.properties")
                .addAsManifestResource(new FileAsset(new File("src/test/resources/jwt/public.pem")), "public.pem");
    }

}
