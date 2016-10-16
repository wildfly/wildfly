/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
