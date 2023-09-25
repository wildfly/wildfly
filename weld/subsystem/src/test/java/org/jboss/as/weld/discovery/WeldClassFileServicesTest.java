/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.discovery;

import java.io.IOException;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.as.weld.discovery.InnerClasses.InnerInterface;
import org.jboss.as.weld.discovery.vetoed.Bravo;
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
    private static ClassFileInfo bravo;
    private static ClassFileInfo charlie;

    @BeforeClass
    public static void init() throws IOException {
        ClassFileServices service = new WeldClassFileServices(IndexUtils.createIndex(Alpha.class, AlphaImpl.class, AbstractAlpha.class, InnerClasses.class,
                Bravo.class, "org/jboss/as/weld/discovery/vetoed/package-info.class", Inject.class, Named.class, Charlie.class), Thread.currentThread()
                .getContextClassLoader());
        alpha = service.getClassFileInfo(Alpha.class.getName());
        abstractAlpha = service.getClassFileInfo(AbstractAlpha.class.getName());
        alphaImpl = service.getClassFileInfo(AlphaImpl.class.getName());
        innerInterface = service.getClassFileInfo(InnerClasses.InnerInterface.class.getName());
        bravo = service.getClassFileInfo(Bravo.class.getName());
        charlie = service.getClassFileInfo(Charlie.class.getName());
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
        Assert.assertTrue(bravo.isVetoed());
    }

    @Test
    public void testSuperclassName() {
        Assert.assertEquals(Object.class.getName(), alpha.getSuperclassName());
        Assert.assertEquals(Object.class.getName(), abstractAlpha.getSuperclassName());
        Assert.assertEquals(AbstractAlpha.class.getName(), alphaImpl.getSuperclassName());
    }

    @Test
    public void testIsAssignableFrom() {
        Assert.assertTrue(alpha.isAssignableFrom(AlphaImpl.class));
        Assert.assertTrue(abstractAlpha.isAssignableFrom(AlphaImpl.class));
        Assert.assertFalse(abstractAlpha.isAssignableFrom(Alpha.class));
        Assert.assertTrue(innerInterface.isAssignableFrom(Bravo.class));
        Assert.assertTrue(alphaImpl.isAssignableFrom(Bravo.class));
    }

    @Test
    public void testIsAssignableTo() {
        Assert.assertTrue(alphaImpl.isAssignableTo(Alpha.class));
        Assert.assertTrue(abstractAlpha.isAssignableTo(Alpha.class));
        Assert.assertFalse(abstractAlpha.isAssignableTo(AlphaImpl.class));
        Assert.assertTrue(bravo.isAssignableTo(InnerInterface.class));
        Assert.assertTrue(bravo.isAssignableTo(AbstractAlpha.class));
        Assert.assertFalse(bravo.isAssignableTo(InnerClasses.class));
    }

    @Test
    public void testIsAssignableToObject() {
        Assert.assertTrue(alpha.isAssignableTo(Object.class));
        Assert.assertTrue(abstractAlpha.isAssignableTo(Object.class));
        Assert.assertTrue(alphaImpl.isAssignableTo(Object.class));
        Assert.assertTrue(bravo.isAssignableTo(Object.class));
    }

    @Test
    public void testIsAssignableFromObject() {
        Assert.assertFalse(alpha.isAssignableFrom(Object.class));
        Assert.assertFalse(abstractAlpha.isAssignableFrom(Object.class));
        Assert.assertFalse(alphaImpl.isAssignableFrom(Object.class));
        Assert.assertFalse(bravo.isAssignableFrom(Object.class));
    }

    @Test
    public void testIsAnnotationDeclared() {
        Assert.assertTrue(alpha.isAnnotationDeclared(Vetoed.class));
        Assert.assertTrue(innerInterface.isAnnotationDeclared(Named.class));
        Assert.assertFalse(bravo.isAnnotationDeclared(Vetoed.class));
        Assert.assertFalse(bravo.isAnnotationDeclared(Named.class));
        Assert.assertFalse(bravo.isAnnotationDeclared(Inject.class));
    }

    @Test
    public void testContainsAnnotation() {
        Assert.assertTrue(alpha.containsAnnotation(Vetoed.class));
        Assert.assertTrue(innerInterface.containsAnnotation(Named.class));
        Assert.assertFalse(bravo.containsAnnotation(Vetoed.class));
        Assert.assertFalse(bravo.containsAnnotation(Named.class));
        Assert.assertTrue(bravo.containsAnnotation(Inject.class));
    }

    @Test
    public void testContainsAnnotationReflectionFallback() {
        Assert.assertTrue(charlie.containsAnnotation(Target.class));
        Assert.assertTrue(bravo.containsAnnotation(Target.class));
    }
}
