/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2022, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.mail.providers;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.mail.Provider;
import jakarta.mail.Session;

@RunWith(Arquillian.class)
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
        Assert.assertNotNull(sessionOne);
        Assert.assertNotNull(sessionTwo);
        Assert.assertNotNull(sessionThree);

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

            Assert.assertTrue("The expected custom providers cannot be found on the default Mail Session", found == 2);
        }
    }
}
