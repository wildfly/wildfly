/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jaxr;

import junit.framework.TestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.jaxr.service.JAXRConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.registry.ConnectionFactory;
import java.util.Properties;

/**
 * Tests the JAXR connection factory bound to JNDI
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
@RunWith(Arquillian.class)
public class JaxrConnectionFactoryBindingTestCase
{
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jaxr-connection-test");
        return archive;
    }

    @Test
    public void testJaxrJNDIConnection() throws Exception
    {
        InitialContext context = new InitialContext();
        String lookup = JAXRConfiguration.JAXR_DEFAULT_CONNECTION_FACTORY_BINDING;
        ConnectionFactory factory = (ConnectionFactory) context.lookup(lookup);
        Assert.assertNotNull("Connection Factory from JNDI:", factory);
    }
}
