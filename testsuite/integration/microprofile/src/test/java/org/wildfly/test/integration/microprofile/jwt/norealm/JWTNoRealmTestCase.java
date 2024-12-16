/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.norealm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.jwt.BaseJWTCase;
import org.wildfly.test.integration.microprofile.jwt.SampleEndPoint;

/**
 * A smoke test case for a simple JWT deployment and invocation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JWTNoRealmTestCase extends BaseJWTCase {

    private static final String DEPLOYMENT_NAME = JWTNoRealmTestCase.class.getSimpleName() + ".war";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addClasses(App.class, SampleEndPoint.class)
                .addAsWebInfResource(BaseJWTCase.class.getPackage(), "web.xml", "web.xml")
                .addAsManifestResource(BaseJWTCase.class.getPackage(),"microprofile-config.properties", "microprofile-config.properties")
                .addAsManifestResource(BaseJWTCase.class.getPackage(), "public.pem", "public.pem");
    }

}
