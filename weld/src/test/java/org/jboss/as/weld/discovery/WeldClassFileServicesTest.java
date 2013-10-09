/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.discovery;

import java.io.IOException;
import java.lang.reflect.Modifier;

import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class WeldClassFileServicesTest {

    private static ClassFileInfo alpha;
    private static ClassFileInfo abstractAlpha;
    private static ClassFileInfo alphaImpl;
    private static ClassFileInfo innerInterface;

    @BeforeClass
    public static void init() throws IOException {
        ClassFileServices service = new WeldClassFileServices(IndexUtils.createIndex(Alpha.class, AlphaImpl.class, AbstractAlpha.class, InnerClasses.class));
        alpha = service.getClassFileInfo(Alpha.class.getName());
        abstractAlpha = service.getClassFileInfo(AbstractAlpha.class.getName());
        alphaImpl = service.getClassFileInfo(AlphaImpl.class.getName());
        innerInterface = service.getClassFileInfo(InnerClasses.InnerInterface.class.getName());
    }

    @Test
    public void testModifiers() throws IOException {
        Assert.assertTrue(Modifier.isAbstract(alpha.getModifiers()));
        Assert.assertTrue(Modifier.isAbstract(abstractAlpha.getModifiers()));
        Assert.assertFalse(Modifier.isAbstract(alphaImpl.getModifiers()));

        Assert.assertFalse(Modifier.isStatic(alpha.getModifiers()));
        Assert.assertFalse(Modifier.isStatic(abstractAlpha.getModifiers()));
        Assert.assertFalse(Modifier.isStatic(alphaImpl.getModifiers()));
    }

    @Test
    public void testVeto() throws IOException {
        Assert.assertTrue(alpha.isVetoed());
        Assert.assertFalse(abstractAlpha.isVetoed());
        Assert.assertFalse(alphaImpl.isVetoed());
    }

    @Test
    public void testSuperclassName() {
        Assert.assertEquals(Object.class.getName(), alpha.getSuperclassName());
        Assert.assertEquals(Object.class.getName(), abstractAlpha.getSuperclassName());
        Assert.assertEquals(AbstractAlpha.class.getName(), alphaImpl.getSuperclassName());
    }

    @Test
    public void testTopLevelClass() {
        Assert.assertTrue(alpha.isTopLevelClass());
        Assert.assertTrue(alpha.isTopLevelClass());
        Assert.assertTrue(alpha.isTopLevelClass());
        Assert.assertFalse(innerInterface.isTopLevelClass());
    }
}
