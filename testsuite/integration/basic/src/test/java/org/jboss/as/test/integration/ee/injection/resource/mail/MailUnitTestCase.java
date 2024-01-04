/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.mail;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing injection of mail service and its definition in xml file.
 * Part migration of tests from EJB testsuite (mail/Mail) [JIRA JBQA-5483].
 *
 * @author Darran Lofthouse, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class MailUnitTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "mail-injection-test.jar");
        jar.addClasses(MailUnitTestCase.class, StatelessMail.class, StatelessMailBean.class);
        jar.addAsManifestResource(MailUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testMailInjection() throws Exception {
        StatelessMail bean = (StatelessMail) ctx.lookup("java:module/StatelessMailBean");
        Assert.assertNotNull(bean);

        bean.testMail();
        bean.testMailInjection();
    }
}
