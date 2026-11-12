/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.cdi;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.mail.Session;

/**
 * Verifies Mail Session can be injected using CDI
 */
@ExtendWith(ArquillianExtension.class)
public class MailSessionCDIInjectionTest {

    @Inject
    Session sessionOne;

    @Inject
    @MethodInjectQualifier
    Session sessionTwo;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "mail-cdi-injection-test.war")
                .addClasses(MailSessionProducer.class, MethodInjectQualifier.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void test() throws IOException, ExecutionException, TimeoutException {
        Assertions.assertNotNull(sessionOne);
        Assertions.assertNotNull(sessionTwo);
    }
}
