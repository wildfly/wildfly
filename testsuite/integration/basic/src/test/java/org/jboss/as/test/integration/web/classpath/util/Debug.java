/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.classpath.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Various debugging utility methods available for use in unit tests
 *
 * @author Scott.Stark@jboss.org
 */
public class Debug {

    /**
     * Format a string buffer containing the Class, Interfaces, CodeSource,
     * and ClassLoader information for the given object
     * clazz.
     *
     * @param clazz the Class
     * @param results, the buffer to write the info to
     */
    public static void displayClassInfo(Class<?> clazz, StringBuffer results) {
        // Print out some codebase info for the ProbeHome
        ClassLoader cl = clazz.getClassLoader();
        results.append("\n" + clazz.getName() + ".ClassLoader=" + cl);
        ClassLoader parent = cl;
        while (parent != null) {
            results.append("\n.." + parent);
            URL[] urls = getClassLoaderURLs(parent);
            int length = urls != null ? urls.length : 0;
            for (int u = 0; u < length; u++) {
                results.append("\n...." + urls[u]);
            }
            if (parent != null)
                parent = parent.getParent();
        }
        CodeSource clazzCS = clazz.getProtectionDomain().getCodeSource();
        if (clazzCS != null)
            results.append("\n++++CodeSource: " + clazzCS);
        else
            results.append("\n++++Null CodeSource");

        results.append("\nImplemented Interfaces:");
        Class<?>[] ifaces = clazz.getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            results.append("\n++" + ifaces[i]);
            ClassLoader loader = ifaces[i].getClassLoader();
            results.append("\n++++ClassLoader: " + loader);
            ProtectionDomain pd = ifaces[i].getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            if (cs != null)
                results.append("\n++++CodeSource: " + cs);
            else
                results.append("\n++++Null CodeSource");
        }
    }

    /**
     * Use reflection to access a URL[] getURLs or ULR[] getAllURLs method so
     * that non-URLClassLoader class loaders, or class loaders that override
     * getURLs to return null or empty, can provide the true classpath info.
     */
    public static URL[] getClassLoaderURLs(ClassLoader cl) {
        URL[] urls = {};
        try {
            Class<?> returnType = urls.getClass();
            Class<?>[] parameterTypes = {};
            Method getURLs = cl.getClass().getMethod("getURLs", parameterTypes);
            if (returnType.isAssignableFrom(getURLs.getReturnType())) {
                Object[] args = {};
                urls = (URL[]) getURLs.invoke(cl, args);
            }
        } catch (Exception ignore) {
        }
        return urls;
    }

    /**
     * Recursively display the naming context information into the buffer.
     *
     * @param ctx
     * @param indent
     * @param buffer
     * @param verbose
     */
    public static void list(Context ctx, String indent, StringBuffer buffer, boolean verbose) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            NamingEnumeration<?> ne = ctx.list("");
            while (ne.hasMore()) {
                NameClassPair pair = (NameClassPair) ne.next();

                String name = pair.getName();
                String className = pair.getClassName();
                boolean recursive = false;
                boolean isLinkRef = false;
                boolean isProxy = false;
                Class<?> c = null;
                try {
                    c = loader.loadClass(className);

                    if (Context.class.isAssignableFrom(c))
                        recursive = true;
                    if (LinkRef.class.isAssignableFrom(c))
                        isLinkRef = true;

                    isProxy = Proxy.isProxyClass(c);
                } catch (ClassNotFoundException cnfe) {
                    // If this is a $Proxy* class its a proxy
                    if (className.startsWith("$Proxy")) {
                        isProxy = true;
                        // We have to get the class from the binding
                        try {
                            Object p = ctx.lookup(name);
                            c = p.getClass();
                        } catch (NamingException e) {
                            Throwable t = e.getRootCause();
                            if (t instanceof ClassNotFoundException) {
                                // Get the class name from the exception msg
                                String msg = t.getMessage();
                                if (msg != null) {
                                    // Reset the class name to the CNFE class
                                    className = msg;
                                }
                            }
                        }
                    }
                }

                buffer.append(indent + " +- " + name);

                // Display reference targets
                if (isLinkRef) {
                    // Get the
                    try {
                        Object obj = ctx.lookupLink(name);

                        LinkRef link = (LinkRef) obj;
                        buffer.append("[link -> ");
                        buffer.append(link.getLinkName());
                        buffer.append(']');
                    } catch (Throwable t) {
                        buffer.append("invalid]");
                    }
                }

                // Display proxy interfaces
                if (isProxy) {
                    buffer.append(" (proxy: " + pair.getClassName());
                    if (c != null) {
                        Class<?>[] ifaces = c.getInterfaces();
                        buffer.append(" implements ");
                        for (int i = 0; i < ifaces.length; i++) {
                            buffer.append(ifaces[i]);
                            buffer.append(',');
                        }
                        buffer.setCharAt(buffer.length() - 1, ')');
                    } else {
                        buffer.append(" implements " + className + ")");
                    }
                } else if (verbose) {
                    buffer.append(" (class: " + pair.getClassName() + ")");
                }

                buffer.append('\n');
                if (recursive) {
                    try {
                        Object value = ctx.lookup(name);
                        if (value instanceof Context) {
                            Context subctx = (Context) value;
                            list(subctx, indent + " |  ", buffer, verbose);
                        } else {
                            buffer.append(indent + " |   NonContext: " + value);
                            buffer.append('\n');
                        }
                    } catch (Throwable t) {
                        buffer.append("Failed to lookup: " + name + ", errmsg=" + t.getMessage());
                        buffer.append('\n');
                    }
                }
            }
            ne.close();
        } catch (NamingException ne) {
            buffer.append("error while listing context " + ctx.toString() + ": " + ne.toString(true));
            formatException(buffer, ne);
        }
    }

    public static void formatException(StringBuffer buffer, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        buffer.append("<pre>\n");
        t.printStackTrace(pw);
        buffer.append(sw.toString());
        buffer.append("</pre>\n");
    }
}
