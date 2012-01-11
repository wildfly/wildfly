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
import java.net.URL;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * A trivial utility class that is placed into the lib/util.jar directory of
 * the war archive and used by servlets in the war to test access to the lib
 * jars.
 *
 * @author Scott.Stark@jboss.org
 */
public class Util {

    public static String getTime() {
        return new Date().toString();
    }

    public static URL configureLog4j() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL webPropsURL = loader.getResource("weblog4j.properties");
        URL web2PropsURL = loader.getResource("web2log4j.properties");
        URL propsURL = loader.getResource("log4j.properties");
        System.out.println("getResource('weblog4j.properties') via TCL = " + webPropsURL);
        System.out.println("getResource('web2log4j.properties') via TCL = " + web2PropsURL);
        System.out.println("getResource('log4j.properties') via TCL = " + propsURL);
        URL webPropsURL2 = Util.class.getResource("/weblog4j.properties");
        URL web2PropsURL2 = Util.class.getResource("/web2log4j.properties");
        URL propsURL2 = Util.class.getResource("/log4j.properties");
        System.out.println("getResource('/weblog4j.properties') via CL = " + webPropsURL2);
        System.out.println("getResource('web2log4j.properties') via CL = " + web2PropsURL2);
        System.out.println("getResource('/log4j.properties') via CL = " + propsURL2);
        return propsURL;
    }

    public static void showTree(String indent, Context ctx, PrintWriter out) throws NamingException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        NamingEnumeration<?> enumeration = ctx.list("");
        while (enumeration.hasMoreElements()) {
            NameClassPair ncp = (NameClassPair) enumeration.next();
            String name = ncp.getName();
            out.print(indent + " +- " + name);
            boolean recursive = false;
            boolean isLinkRef = false;
            try {
                Class<?> c = loader.loadClass(ncp.getClassName());
                if (Context.class.isAssignableFrom(c))
                    recursive = true;
                if (LinkRef.class.isAssignableFrom(c))
                    isLinkRef = true;
            } catch (ClassNotFoundException cnfe) {
            }

            if (isLinkRef) {
                try {
                    LinkRef link = (LinkRef) ctx.lookupLink(name);
                    out.print("[link -> ");
                    out.print(link.getLinkName());
                    out.print(']');
                } catch (Throwable e) {
                    out.print("[invalid] (" + e.getMessage() + ")");
                }
            }
            out.println();

            if (recursive) {
                try {
                    Object value = ctx.lookup(name);
                    if (value instanceof Context) {
                        Context subctx = (Context) value;
                        showTree(indent + " |  ", subctx, out);
                    } else {
                        out.println(indent + " |   NonContext: " + value);
                    }
                } catch (Throwable t) {
                    out.println("Failed to lookup: " + name + ", errmsg=" + t.getMessage());
                }
            }

        }
    }

    public static void dumpClassLoader(ClassLoader cl, PrintWriter out) {
        int level = 0;
        while (cl != null) {
            String msg = "Servlet ClassLoader[" + level + "]: " + cl.getClass().getName() + ':' + cl.hashCode();
            out.println(msg);
            URL[] urls = getClassLoaderURLs(cl);
            msg = "  URLs:";
            out.println(msg);
            for (int u = 0; u < urls.length; u++) {
                msg = "  [" + u + "] = " + urls[u];
                out.println(msg);
            }
            cl = cl.getParent();
            level++;
        }
    }

    public static void dumpENC(PrintWriter out) throws NamingException {
        InitialContext iniCtx = new InitialContext();
        Context enc = (Context) iniCtx.lookup("java:comp/env");
        showTree("", enc, out);
    }

    public static String displayClassLoaders(ClassLoader cl) throws NamingException {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        dumpClassLoader(cl, out);
        return sw.toString();
    }

    public static String displayENC() throws NamingException {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        dumpENC(out);
        return sw.toString();
    }

    /**
     * Use reflection to access a URL[] getURLs method so that
     * non-URLClassLoader class loaders that support this method can provide
     * info.
     */
    private static URL[] getClassLoaderURLs(ClassLoader cl) {
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
}
