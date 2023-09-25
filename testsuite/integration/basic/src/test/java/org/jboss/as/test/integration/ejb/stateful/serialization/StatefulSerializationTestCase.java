/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
