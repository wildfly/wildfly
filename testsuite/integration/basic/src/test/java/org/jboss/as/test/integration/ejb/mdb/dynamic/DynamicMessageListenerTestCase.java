/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.mdb.dynamic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.mdb.dynamic.adapter.TelnetResourceAdapter;
import org.jboss.as.test.integration.ejb.mdb.dynamic.api.TelnetListener;
import org.jboss.as.test.integration.ejb.mdb.dynamic.application.MyMdb;
import org.jboss.as.test.integration.ejb.mdb.dynamic.impl.TelnetServer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DynamicMessageListenerTestCase {
    @Deployment
    public static Archive createDeplyoment() {
        final EnterpriseArchive ear = create(EnterpriseArchive.class, "ear-with-rar.ear")
                .addAsModule(create(JavaArchive.class, "telnet-ra.rar")
                        .addAsManifestResource(TelnetResourceAdapter.class.getPackage(), "ra.xml", "ra.xml")
                        .addPackages(true, TelnetResourceAdapter.class.getPackage(), TelnetListener.class.getPackage(), TelnetServer.class.getPackage()))
                .addAsModule(create(JavaArchive.class, "mdb.jar")
                        .addClasses(MyMdb.class));
        return ear;
    }

    @Test
    public void test1() throws Exception {
        Thread.currentThread().sleep(5000);
    }
}
