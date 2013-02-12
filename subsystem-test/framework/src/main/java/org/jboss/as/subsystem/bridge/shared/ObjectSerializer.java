/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.subsystem.bridge.shared;

import java.io.IOException;

import org.jboss.as.subsystem.bridge.impl.ObjectSerializerImpl;

/**
 * This interface will only be loaded up by the app classloader. It is used by both the app and the childfirst classloaders,
 * hence the use of Object for parameters
 *
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ObjectSerializer {

    byte[] serializeModelNode(Object object) throws IOException;

    Object deserializeModelNode(byte[] object) throws IOException;

    String serializeModelVersion(Object object);

    Object deserializeModelVersion(String object);

    byte[] serializeAdditionalInitialization(Object object) throws IOException;

    Object deserializeAdditionalInitialization(byte[] object) throws IOException, ClassNotFoundException;

    byte[] serializeModelTestOperationValidatorFilter(Object object) throws IOException;

    Object deserializeModelTestOperationValidatorFilter(byte[] object) throws IOException, ClassNotFoundException;

    public static class FACTORY {
        public static ObjectSerializer createSerializer(ClassLoader classLoader) {
            try {
                Class<?> clazz = classLoader.loadClass(ObjectSerializerImpl.class.getName());
                return (ObjectSerializer)clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
