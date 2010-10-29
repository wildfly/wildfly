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

package org.jboss.as.connector.jndi;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.resource.Referenceable;
import org.jboss.as.naming.context.ModularReference;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.logging.Logger;
import org.jboss.util.naming.Util;

/**
 *
 * @author John Bailey
 */
public class AS7JndiStrategy implements JndiStrategy {
    private static Logger log = Logger.getLogger("org.jboss.as.connector.jndi");

    private static boolean trace = log.isTraceEnabled();

    private static ConcurrentMap<String, Object> objs = new ConcurrentHashMap<String, Object>();

    /**
     * Obtain the connection factory
     *
     * {@inheritDoc}
     */
    public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
        final Reference ref = (Reference) obj;
        final String className = (String) ref.get("class").getContent();
        final String cfname = (String) ref.get("name").getContent();
        return objs.get(qualifiedName(cfname, className));
    }

    /** {@inheritDoc} */
    public String[] bindConnectionFactories(final String deployment, final Object[] cfs) throws Throwable {
        throw new IllegalStateException("JNDI names are required");
    }

    /** {@inheritDoc} */
    public String[] bindConnectionFactories(final String deployment, final Object[] cfs, final String[] jndis) throws Throwable {
        ensureNotEmpty(deployment, "Deployment");
        ensureNotEmpty(cfs, "CFS");
        ensureNotEmpty(jndis, "JNDIs");

        if (cfs.length != jndis.length)
            throw new IllegalArgumentException("Number of connection factories doesn't match number of JNDI names");

        for (int i = 0; i < cfs.length; i++) {
            final String jndiName = jndis[i];
            final Object cf = cfs[i];
            bindObject(jndiName, cf);
        }
        return jndis;
    }

    /** {@inheritDoc} */
    public void unbindConnectionFactories(final String deployment, final Object[] cfs) throws Throwable {
        throw new IllegalStateException("JNDI names are required");
    }

    /** {@inheritDoc} */
    public void unbindConnectionFactories(final String deployment, final Object[] cfs, final String[] jndis) throws Throwable {
        ensureNotEmpty(cfs, "CFS");
        ensureNotEmpty(jndis, "JNDIs");

        if (cfs.length != jndis.length)
            throw new IllegalArgumentException("Number of connection factories doesn't match number of JNDI names");

        for (int i = 0; i < cfs.length; i++) {
            String jndiName = jndis[i];
            Object cf = cfs[i];

            unbindObject(jndiName, cf);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] bindAdminObjects(String deployment, Object[] aos) throws Throwable {
        throw new IllegalStateException("JNDI names are required");
    }

    /**
     * {@inheritDoc}
     */
    public String[] bindAdminObjects(String deployment, Object[] aos, String[] jndis) throws Throwable {
        ensureNotEmpty(deployment, "Deployment");
        ensureNotEmpty(aos, "AOS");
        ensureNotEmpty(jndis, "JNDIs");

        if (aos.length != jndis.length)
            throw new IllegalArgumentException("Number of admin objects doesn't match number of JNDI names");

        for (int i = 0; i < aos.length; i++) {
            String jndiName = jndis[i];
            Object ao = aos[i];

            bindObject(jndiName, ao);
        }
        return jndis;
    }

    /** {@inheritDoc} */
    public void unbindAdminObjects(String deployment, Object[] aos) throws Throwable {
        throw new IllegalStateException("JNDI names are required");
    }

    /** {@inheritDoc} */
    public void unbindAdminObjects(final String deployment, final Object[] aos, final String[] jndis) throws Throwable {
        ensureNotEmpty(aos, "AOS");
        ensureNotEmpty(jndis, "JNDIs");

        if (aos.length != jndis.length)
            throw new IllegalArgumentException("Number of admin objects doesn't match number of JNDI names");

        for (int i = 0; i < aos.length; i++) {
            final String jndiName = jndis[i];
            final Object ao = aos[i];

            unbindObject(jndiName, ao);
        }
    }

    private void bindObject(final String jndiName, final Object obj) throws Exception {
        ensureNotNull(obj, "Bind Object");
        ensureNotEmpty(jndiName, "JNDI Name");

        final String className = obj.getClass().getName();
        log.tracef("Binding %s under %s", className, jndiName);

        final Reference reference = ModularReference.create(className, new StringRefAddr("class", className), AS7JndiStrategy.class);
        reference.add(new StringRefAddr("name", jndiName));

        if (objs.putIfAbsent(qualifiedName(jndiName, className), obj) != null)
            throw new Exception("Deployment " + className + " failed, " + jndiName + " is already deployed");

        final Referenceable referenceable = (Referenceable) obj;
        referenceable.setReference(reference);

        final Context context = new InitialContext();
        try {
            Util.bind(context, jndiName, obj);
            log.debugf("Bound %s under %s", className, jndiName);
        } finally {
            safeClose(context);
        }
    }

    private void unbindObject(final String jndiName, final Object object) throws Exception {
        ensureNotNull(object, "Bind Object");
        ensureNotEmpty(jndiName, "JNDI Name");

        final String className = object.getClass().getName();
        log.tracef("Unbinding %s  under %s", className, jndiName);

        final Context context = new InitialContext();
        try {
            Util.unbind(context, jndiName);

            objs.remove(qualifiedName(jndiName, className));

            log.debugf("Unbound %s under %s", className, jndiName);
        } catch (Throwable t) {
            log.warn("Exception during unbind", t);
        } finally {
            safeClose(context);
        }
    }

    /**
     * Clone the JNDI strategy implementation
     *
     * @return A copy of the implementation
     * @throws CloneNotSupportedException Thrown if the copy operation isn't supported
     */
    public JndiStrategy clone() throws CloneNotSupportedException {
        return (JndiStrategy) super.clone();
    }

    private static String qualifiedName(String name, String className) {
        return className + "#" + name;
    }

    private void safeClose(final Context context) {
        if (context != null) try {
            context.close();
        }
        catch (NamingException ignored) {
        }
    }

    private void ensureNotNull(final Object object, final String name) {
        if (object == null) throw new IllegalArgumentException(name + " is null");
    }

    private void ensureNotEmpty(final String string, final String name) {
        ensureNotNull(string, name);
        if(string.isEmpty())
            throw new IllegalArgumentException(name + " is empty");
    }

    private void ensureNotEmpty(final Object[] array, final String name) {
        ensureNotNull(array, name);
        if(array.length == 0) throw new IllegalArgumentException(name + " is empty");
    }
}
