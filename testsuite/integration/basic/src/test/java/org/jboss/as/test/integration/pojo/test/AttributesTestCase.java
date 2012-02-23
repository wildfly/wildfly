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

package org.jboss.as.test.integration.pojo.test;

import java.lang.reflect.Method;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.pojo.support.WithAttributes;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 */
@RunWith(Arquillian.class)
public class AttributesTestCase {
    @Deployment(name = "attributes-beans")
    public static JavaArchive getSimpleBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "attributes-beans.jar");
        archive.addClass(WithAttributes.class);
        archive.addAsManifestResource("pojo/attributes-jboss-beans.xml", "attributes-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("attributes-beans")
    public void testSimpleBeans() throws Exception {
        String [] attributeNames = {"Boolean","Char","Byte","Short"
                ,"Integer","Long","Float","Double","AtomicBoolean"
                ,"AtomicInteger","AtomicLong","BigDecimal"};
//        Object o = null;
//        Class oClass = null;
//       
//        for(String attrbiuteName:attributeNames)
//        {
//            Method getMethod = oClass.getMethod("get"+attrbiuteName, null);
//            Assert.assertNotNull("No getter method found for attribute '"+attrbiuteName+"'",getMethod);
//            Object attributeValue = getMethod.invoke(o, null);
//            Assert.assertNotNull("Found null attribute value for '"+attrbiuteName+"'",attributeValue);
//        }
    }
}
