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
package org.wildfly.test.integration.microprofile.opentracing;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.test.integration.microprofile.opentracing.application.TracerIdentityApplication;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;


/**
 * Test verifying the assumption that different services inside single EAR have different tracers.
 *
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarOpenTracingTestCase extends AbstractEarOpenTracingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive serviceOne = ShrinkWrap.create(WebArchive.class, "ServiceOne.war")
            .addClass(TracerIdentityApplication.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        WebArchive serviceTwo = ShrinkWrap.create(WebArchive.class, "ServiceTwo.war")
            .addClass(TracerIdentityApplication.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "EarOpenTracingTestCase.ear")
            .addAsModules(serviceOne, serviceTwo);
        return ear;
    }
}
