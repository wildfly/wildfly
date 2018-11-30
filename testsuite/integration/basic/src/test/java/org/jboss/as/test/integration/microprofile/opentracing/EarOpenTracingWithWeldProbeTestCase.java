/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.microprofile.opentracing;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.microprofile.opentracing.application.TracerIdentityApplication;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Same test case as {@link EarOpenTracingTestCase} only adding Weld Probe to the mix. For monitoring purposes, Probe forces
 * creation of Weld subclasses on all beans, including those coming from MP OpenTracing extension
 *
 * @see WFLY-11432
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarOpenTracingWithWeldProbeTestCase extends AbstractEarOpenTracingTestCase {

    private static String DEVEL_MODE_STRING = "<web-app version=\"2.5\"\n"
        + "xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
        + "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee\n"
        + "http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\">\n<context-param>\n"
        + "<param-name>org.jboss.weld.development</param-name>\n"
        + "<param-value>true</param-value>\n</context-param>\n"
        + "</web-app>";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive serviceOne = ShrinkWrap.create(WebArchive.class, "ServiceOne.war")
            .addClass(TracerIdentityApplication.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new StringAsset(DEVEL_MODE_STRING), "web.xml");
        WebArchive serviceTwo = ShrinkWrap.create(WebArchive.class, "ServiceTwo.war")
            .addClass(TracerIdentityApplication.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new StringAsset(DEVEL_MODE_STRING), "web.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "EarOpenTracingTestCase.ear")
            .addAsModules(serviceOne, serviceTwo);
        return ear;
    }

}
