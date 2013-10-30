/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.definitions;

import static org.jboss.shrinkwrap.api.ArchivePaths.create;

import javax.ejb.EJB;
import javax.jms.JMSException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
public class JMSResourceDefinitionsTestCase {

    @EJB
    private MessagingBean bean;

    @Deployment
    public static JavaArchive createArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "JMSResourceDefinitionsTestCase.jar")
                .addPackage(MessagingBean.class.getPackage())
                .addAsManifestResource(
                        MessagingBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        create("beans.xml"));
        //System.out.println("archive = " + archive.toString(true));
        return archive;
    }

    @Test
    public void testInjectedDefinitions() throws JMSException {
        bean.checkInjectedResources();
    }
}
