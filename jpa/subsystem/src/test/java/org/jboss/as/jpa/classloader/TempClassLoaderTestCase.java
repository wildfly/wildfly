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

package org.jboss.as.jpa.classloader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.persistence.Entity;

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
        assertTrue(entity.getClass().isAnnotationPresent(Entity.class));
        assertTrue(entityClass == tempClassLoader.loadClass(className));
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
