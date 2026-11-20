/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.providers;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.mail.Provider;
import jakarta.mail.Session;

@ExtendWith(ArquillianExtension.class)
public class MailSessionCustomProviderTestCase {

    @Resource(lookup = "java:jboss/mail/Default")
    private Session sessionOne;

    @Inject
    private Session sessionTwo;

    @Inject
    @MethodInjectQualifier
    private Session sessionThree;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(MailSessionProducer.class, MethodInjectQualifier.class)
                .addAsResource(MailSessionCustomProviderTestCase.class.getPackage(),"javamail.providers", "META-INF/javamail.providers")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testCustomProvidersInApplication() {
        Assertions.assertNotNull(sessionOne);
        Assertions.assertNotNull(sessionTwo);
        Assertions.assertNotNull(sessionThree);

        List<Session> sessions = List.of(sessionOne, sessionTwo, sessionThree);

        for(Session session : sessions) {
            Provider[] providers = session.getProviders();

            int found = 0;
            for (Provider p : providers) {
                if (p.toString().equals("jakarta.mail.Provider[TRANSPORT,CustomTransport,org.jboss.qa.management.mail.custom.CustomTransport,JBoss QE]")) {
                    found++;
                }
                if (p.toString().equals("jakarta.mail.Provider[STORE,CustomStore,org.jboss.qa.management.mail.custom.CustomStore,JBoss QE]")) {
                    found++;
                }
            }

            Assertions.assertTrue(found == 2, "The expected custom providers cannot be found on the default Mail Session");
        }
    }
}
