/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.http;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.MechanismRealmConfiguration;

/**
 * Test of DIGEST-SHA-256 HTTP mechanism.
 *
 * @author Jan Kalina
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DigestSha256MechTestCase.ServerSetup.class })
public class DigestSha256MechTestCase extends PasswordMechTestBase {

    private static final String NAME = DigestSha256MechTestCase.class.getSimpleName();

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .addClasses(SimpleServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(APP_DOMAIN), "jboss-web.xml")
                .addAsWebInfResource(DigestSha256MechTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    static class ServerSetup extends AbstractMechTestBase.ServerSetup {
        @Override protected MechanismConfiguration getMechanismConfiguration() {
            return MechanismConfiguration.builder()
                    .withMechanismName("DIGEST-SHA-256")
                    .addMechanismRealmConfiguration(MechanismRealmConfiguration.builder()
                            .withRealmName("Digest SHA-256 kingdom")
                            .build())
                    .build();
        }
    }
}
