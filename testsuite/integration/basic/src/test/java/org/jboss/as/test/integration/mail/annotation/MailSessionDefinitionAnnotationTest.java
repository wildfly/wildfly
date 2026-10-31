/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.annotation;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
@ExtendWith(ArquillianExtension.class)
public class MailSessionDefinitionAnnotationTest {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mail-injection-test.jar");
        jar.addClasses(MailSessionDefinitionAnnotationTest.class, StatelessMail.class, MailDefiner.class);
        jar.addAsManifestResource(MailSessionDefinitionAnnotationTest.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testMailInjection() throws Exception {
        InitialContext ctx = new InitialContext();
        StatelessMail statelessMail = (StatelessMail) ctx.lookup("java:module/" + StatelessMail.class.getSimpleName());
        Assertions.assertNotNull(statelessMail);

        statelessMail.testMailInjection();
    }
}
