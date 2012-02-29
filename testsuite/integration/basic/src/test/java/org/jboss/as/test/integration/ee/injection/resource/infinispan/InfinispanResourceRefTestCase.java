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

package org.jboss.as.test.integration.ee.injection.resource.infinispan;

import java.io.InputStream;
import java.util.jar.JarFile;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class InfinispanResourceRefTestCase {
    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "infinispan-resource-ref.war");
        war.addClasses(InfinispanBean.class, InfinispanResourceRefTestCase.class);
        war.addAsWebInfResource(getWebXml(), "web.xml");
        war.add(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.infinispan export");
                return builder.openStream();
            }
        }, JarFile.MANIFEST_NAME);
        return war;
    }

    @Test
    public void test() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/infinispan");
        Assert.assertTrue(result instanceof InfinispanBean);
        InfinispanBean bean = (InfinispanBean) result;

        bean.test();
    }

    private static StringAsset getWebXml() {
        return new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<web-app version=\"3.0\" metadata-complete=\"false\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\">\n"
                + "    <resource-ref>\n"
                + "        <res-ref-name>" + InfinispanBean.CONTAINER_REF_NAME + "</res-ref-name>\n"
                + "        <lookup-name>java:jboss/infinispan/container/hibernate</lookup-name>\n"
                + "        <injection-target>"
                + "            <injection-target-class>" + InfinispanBean.class.getName() + "</injection-target-class>"
                + "            <injection-target-name>container</injection-target-name>"
                + "        </injection-target>\n"
                + "    </resource-ref>\n"
                + "</web-app>");
    }
}
