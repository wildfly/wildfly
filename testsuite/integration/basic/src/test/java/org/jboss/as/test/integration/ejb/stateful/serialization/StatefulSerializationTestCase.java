/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.serialization;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Tests that the stateful timeout annotation works
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class StatefulSerializationTestCase {

    private static final String ARCHIVE_NAME = "StatefulTimeoutTestCase";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(StatefulSerializationTestCase.class.getPackage());
        return jar;
    }

    protected <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    @Test
    public void testSerializingSfsbProxy() throws Exception {

        AnnotatedBean sfsb1 = lookup(AnnotatedBean.class);
        sfsb1.setValue(10);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(bytes);
        stream.writeObject(sfsb1);
        stream.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        AnnotatedBean bean = (AnnotatedBean) in.readObject();
        in.close();
        Assert.assertEquals(10, bean.getValue());
    }


}
