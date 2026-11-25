/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context;

import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

import com.sun.faces.context.SessionMap;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.immutable.Immutability;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Immutability.class)
public class ContextImmutability  implements Immutability {

    static Object createMutex() {
        // Capture private Mutex object/class via public method
        AtomicReference<Object> mutex = new AtomicReference<>();
        SessionMap.createMutex(new HttpSession() {
            @Override
            public void setAttribute(String name, Object value) {
                mutex.setPlain(value);
            }

            @Override
            public long getCreationTime() {
                return 0;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public long getLastAccessedTime() {
                return 0;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public void setMaxInactiveInterval(int interval) {
            }

            @Override
            public int getMaxInactiveInterval() {
                return 0;
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return null;
            }

            @Override
            public void removeAttribute(String name) {
            }

            @Override
            public void invalidate() {
            }

            @Override
            public boolean isNew() {
                return false;
            }
        });
        return mutex.getPlain();
    }

    private final Immutability immutability = Immutability.instanceOf(List.of(createMutex().getClass()));

    @Override
    public boolean test(Object object) {
        return this.immutability.test(object);
    }
}
