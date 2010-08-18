/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model.base.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * {@link ReadElementCallBack} implementation that constructs an instance of
 * a provided class that exposes a single argument constructor that takes an 
 * {@link XMLExtendedStreamReader} as the single argument.
 * 
 * @author Brian Stansberry
 */
public class SimpleReadElementCallback<T extends AbstractModelElement<T>> implements ReadElementCallback<T> {
    
    private final Constructor<T> ctor;
    
    /**
     * Static factory for the callback.
     * 
     * @param <T> the type of the object the callback will return
     * @param clazz class of type T
     * 
     * @return a callback that will return an instance of T when given an
     *         XMLExtendedStreamReader
     * 
     * @throws NoSuchMethodException if clazz does not expose a single argument
     *              constructor that takes an {@link XMLExtendedStreamReader} 
     *              as the single argument
     */
    public static <T extends AbstractModelElement<T>> ReadElementCallback<T> getCallback(Class<T> clazz) throws NoSuchMethodException {
        return new SimpleReadElementCallback<T>(clazz);
    }
    
    private SimpleReadElementCallback(Class<T> clazz) throws NoSuchMethodException {
        this.ctor = clazz.getConstructor(XMLExtendedStreamReader.class);
    }
    
    @Override
    public T readElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        try {
            return ctor.newInstance(reader);
        } catch (RuntimeException e) {
            throw e;
        } catch (InvocationTargetException e)  {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof XMLStreamException) {
                throw (XMLStreamException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}