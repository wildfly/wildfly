/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.classloader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import jakarta.persistence.Entity;

import org.junit.Test;

/**
 * @author Antti Laisi
 */
public class TempClassLoaderTestCase {

    private TempClassLoaderFactoryImpl factory = new TempClassLoaderFactoryImpl(getClass().getClassLoader());

    @Test
    public void testLoadEntityClass() throws Exception {
        ClassLoader tempClassLoader = factory.createNewTempClassLoader();
        String className = TestEntity.class.getName();

        Class<?> entityClass = tempClassLoader.loadClass(className);
        Object entity = entityClass.newInstance();

        assertFalse(entityClass.equals(TestEntity.class));
        assertFalse(entity instanceof TestEntity);
        assertTrue("entity.getClass() is not a Persistence.Entity (" + annotations(entity.getClass()) + ")", entity.getClass().isAnnotationPresent(Entity.class));
        assertTrue(entityClass == tempClassLoader.loadClass(className));
    }

    private String annotations(Class<?> aClass) {
        for(Annotation annotation : aClass.getAnnotations()) {
            if (annotation.toString().contains(Entity.class.getName()))
                return "found " + Entity.class.getName();
        }
        return aClass.getName() + " is missing annotation " + Entity.class.getName();
    }

    @Test
    public void testLoadResources() throws IOException {
        ClassLoader tempClassLoader = factory.createNewTempClassLoader();
        String resource = TestEntity.class.getName().replace('.', '/') + ".class";

        assertNotNull(tempClassLoader.getResource(resource));
        assertTrue(tempClassLoader.getResources(resource).hasMoreElements());

        InputStream resourceStream = tempClassLoader.getResourceAsStream(resource);
        assertNotNull(resourceStream);
        resourceStream.close();
    }

    @Test
    public void testLoadEntityClassPackage() throws Exception {
        ClassLoader tempClassLoader = factory.createNewTempClassLoader();
        String className = TestEntity.class.getName();

        Class<?> entityClass = tempClassLoader.loadClass(className);
        assertNotNull("could not load package for entity class that came from NewTempClassLoader", entityClass.getPackage());
    }

    @Entity
    public static class TestEntity {
    }

}
