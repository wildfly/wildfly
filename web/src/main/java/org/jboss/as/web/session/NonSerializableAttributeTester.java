/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 * Diagnostic tool to help identify when non-serializable objects are placed in a web session. To use, place this class on the
 * webapp's classpath and add the following to <code>web.xml</code>:
 * <p>
 * <code><listener><listener-class>org.jboss.web.tomcat.service.session.NonSerializableAttributeTester</listener-class></listener></code>
 * </p>
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 82920 $
 */
public class NonSerializableAttributeTester implements HttpSessionAttributeListener {

    public NonSerializableAttributeTester() {
        super();
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        testAttributeSerializability(event.getName(), event.getValue());
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        // do nothing
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent event) {
        testAttributeSerializability(event.getName(), event.getValue());
    }

    private void testAttributeSerializability(String name, Object value) {
        if (!testSerializability(value)) {
            System.out.println(name + " of type " + value.getClass() + " cannot be serialized");
            testRecursively(value);
        }

    }

    private boolean testSerializability(Object obj) {
        if (obj == null)
            return true;

        if (!(obj instanceof Serializable)) {
            return false;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(obj);
                oos.close();
            } catch (NotSerializableException io) {
                return false;
            } catch (IOException io) {
                System.out.println("Unexpected IOException");
                io.printStackTrace(System.out);
            }
        }

        return true;
    }

    private void testRecursively(Object obj) {
        if (obj instanceof Collection<?>) {
            testCollectionSerializability((Collection<?>) obj);
        } else if (obj instanceof Map<?, ?>) {
            testMapSerializability((Map<?, ?>) obj);
        }

    }

    private void testCollectionSerializability(Collection<?> coll) {
        System.out.println("Testing Collection elements");
        int i = 0;
        for (Iterator<?> iter = coll.iterator(); iter.hasNext(); i++) {
            Object obj = iter.next();
            if (!(testSerializability(obj))) {
                System.out.println("Element " + i + " of type " + obj.getClass() + " cannot be serialized");
                testRecursively(obj);
            }
        }
    }

    private void testMapSerializability(Map<?, ?> map) {
        System.out.println("Testing Map entries");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!testSerializability(entry.getKey())) {
                System.out.println("Map Key " + entry.getKey() + " of type " + entry.getKey().getClass()
                        + " cannot be serialized");
                testRecursively(entry.getKey());
            } else if (!testSerializability(entry.getValue())) {
                System.out.println("Map value under Key " + entry.getKey() + " of type " + entry.getValue().getClass()
                        + " cannot be serialized");
                testRecursively(entry.getValue());
            }
        }
    }

}
