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

package org.jboss.as.naming.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author John Bailey
 */
public class JndiView implements JndiViewMBean, Service<JndiView> {
    private static final Logger log = Logger.getLogger("org.jboss.as.naming");
    private static final String OBJECT_NAME = "jboss:type=JNDIView";

    private final InjectedValue<MBeanServer> mbeanServerValue = new InjectedValue<MBeanServer>();

    public synchronized void start(StartContext context) throws StartException {
        final MBeanServer mbeanServer = getMbeanServer();
        try {
            mbeanServer.registerMBean(this, new ObjectName(OBJECT_NAME));
        } catch (Exception e) {
            throw new StartException("Failed to register JndiView mbean.", e);
        }
    }

    public synchronized void stop(StopContext context) {
        final MBeanServer mbeanServer = getMbeanServer();
        try {
            mbeanServer.unregisterMBean(new ObjectName(OBJECT_NAME));
        } catch (Exception e) {
            log.error("Failed to unregister JndiView mbean", e);
        }
    }

    public JndiView getValue() throws IllegalStateException {
        return this;
    }

    public Injector<MBeanServer> getMBeanServerInjector() {
        return mbeanServerValue;
    }

    /**
     * List deployed application java:comp namespaces, the java:
     * namespace as well as the global InitialContext JNDI namespace.
     *
     * @param verbose, if true, list the class of each object in addition to its name
     */
    public String list(boolean verbose) {
        StringBuffer buffer = new StringBuffer(4096);
        Context context = null;
        // List the java: namespace
        try {
            context = new InitialContext();
            context = (Context) context.lookup("java:");
            buffer.append("<h1>java: Namespace</h1>\n");
            buffer.append("<pre>\n");
            list(context, " ", buffer, verbose);
            buffer.append("</pre>\n");
        }
        catch (NamingException e) {
            log.error("lookup for java: failed", e);
            buffer.append("Failed to get InitialContext, ").append(e.toString(true));
            formatException(buffer, e);
        }

        // List the global JNDI namespace
        try {
            context = new InitialContext();
            buffer.append("<h1>Global JNDI Namespace</h1>\n");
            buffer.append("<pre>\n");
            list(context, " ", buffer, verbose);
            buffer.append("</pre>\n");
        }
        catch (NamingException e) {
            log.error("Failed to get InitialContext", e);
            buffer.append("Failed to get InitialContext, ").append(e.toString(true));
            formatException(buffer, e);
        }
        return buffer.toString();
    }

    /**
     * List deployed application java:comp namespaces, the java:
     * namespace as well as the global InitialContext JNDI namespace in a
     * XML Format.
     */
    public String listXML() {
        StringBuffer buffer = new StringBuffer(4096);
        Context context = null;
        openJndiTag(buffer);

        // List the java: namespace
        try {
            context = new InitialContext();
            context = (Context) context.lookup("java:");
        }
        catch (NamingException e) {
            log.error("Failed to get InitialContext for (java:)", e);
            appendErrorTag(buffer,
                    "Failed to get InitialContext for (java:), " +
                            e.toString(true));
        }

        if (context != null) {
            openContextTag(buffer);
            appendJavaNameTag(buffer);
            try {
                listXML(context, buffer);
            }
            catch (Throwable t) {
                log.error("Failed to list contents of (java:)", t);
                appendErrorTag(buffer,
                        "Failed to list contents of (java:), " +
                                t.toString());
            }
            closeContextTag(buffer);

        }   //   if ( context != null )

        // List the global JNDI namespace
        try {
            context = new InitialContext();
        }
        catch (NamingException e) {
            log.error("Failed to get InitialContext", e);
            appendErrorTag(buffer,
                    "Failed to get InitialContext, " + e.toString(true));
        }

        if (context != null) {
            openContextTag(buffer);
            appendGlobalNameTag(buffer);
            try {
                listXML(context, buffer);
            }
            catch (Throwable t) {
                log.error("Failed to list global contents ", t);
                appendErrorTag(buffer,
                        "Failed to list global contents, " + t.toString());
            }
            closeContextTag(buffer);

        }   //   if ( context != null )

        closeJndiTag(buffer);

        return buffer.toString();
    }

    private void list(Context ctx, String indent, StringBuffer buffer, boolean verbose) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            NamingEnumeration<?> ne = ctx.list("");
            while (ne.hasMore()) {
                NameClassPair pair = (NameClassPair) ne.next();
                log.trace("pair: " + pair);

                String name = pair.getName();
                String className = pair.getClassName();
                boolean recursive = false;
                boolean isLinkRef = false;
                boolean isProxy = false;
                Class<?> c = null;
                try {
                    c = loader.loadClass(className);
                    log.trace("type: " + c);

                    if (Context.class.isAssignableFrom(c))
                        recursive = true;
                    if (LinkRef.class.isAssignableFrom(c))
                        isLinkRef = true;

                    isProxy = Proxy.isProxyClass(c);
                }
                catch (ClassNotFoundException cnfe) {
                    // If this is a $Proxy* class its a proxy
                    if (className.startsWith("$Proxy")) {
                        isProxy = true;
                        // We have to get the class from the binding
                        try {
                            Object p = ctx.lookup(name);
                            c = p.getClass();
                        }
                        catch (NamingException e) {
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

                buffer.append(indent).append(" +- ").append(name);

                // Display reference targets
                if (isLinkRef) {
                    // Get the
                    try {
                        log.trace("looking up LinkRef; name=" + name);
                        Object obj = ctx.lookupLink(name);
                        log.trace("Object type: " + obj.getClass());

                        LinkRef link = (LinkRef) obj;
                        buffer.append("[link -> ");
                        buffer.append(link.getLinkName());
                        buffer.append(']');
                    }
                    catch (Throwable t) {
                        log.debug("Invalid LinkRef for: " + name, t);
                        buffer.append("invalid]");
                    }
                }

                // Display proxy interfaces
                if (isProxy) {
                    buffer.append(" (proxy: ").append(pair.getClassName());
                    if (c != null) {
                        Class<?>[] ifaces = c.getInterfaces();
                        buffer.append(" implements ");
                        for (Class<?> iface : ifaces) {
                            buffer.append(iface);
                            buffer.append(',');
                        }
                        buffer.setCharAt(buffer.length() - 1, ')');
                    } else {
                        buffer.append(" implements ").append(className).append(")");
                    }
                } else if (verbose) {
                    buffer.append(" (class: ").append(pair.getClassName()).append(")");
                }

                buffer.append('\n');
                if (recursive) {
                    try {
                        Object value = ctx.lookup(name);
                        if (value instanceof Context) {
                            Context subctx = (Context) value;
                            list(subctx, indent + " |  ", buffer, verbose);
                        } else {
                            buffer.append(indent).append(" |   NonContext: ").append(value);
                            buffer.append('\n');
                        }
                    }
                    catch (Throwable t) {
                        buffer.append("Failed to lookup: ").append(name).append(", errmsg=").append(t.getMessage());
                        buffer.append('\n');
                    }
                }
            }
            ne.close();
        }
        catch (NamingException ne) {
            buffer.append("error while listing context ").append(ctx.toString()).append(": ").append(ne.toString(true));
            formatException(buffer, ne);
        }
    }

    private void listXML(Context ctx, StringBuffer buffer) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            NamingEnumeration<?> ne = ctx.list("");
            while (ne.hasMore()) {
                NameClassPair pair = (NameClassPair) ne.next();
                boolean recursive = false;
                boolean isLinkRef = false;
                try {
                    Class<?> c = loader.loadClass(pair.getClassName());
                    if (Context.class.isAssignableFrom(c))
                        recursive = true;
                    if (LinkRef.class.isAssignableFrom(c))
                        isLinkRef = true;
                }
                catch (ClassNotFoundException cnfe) {
                }

                String name = pair.getName();
                if (isLinkRef) {
                    Object obj = null;
                    LinkRef link = null;
                    try {
                        obj = ctx.lookupLink(name);
                        link = (LinkRef) obj;
                    }
                    catch (Throwable t) {
                        log.error("Invalid LinkRef for: " + name, t);

                        appendLinkRefErrorTag(buffer);
                    }

                    appendLinkRefTag(buffer, link, pair);
                } else {
                    if (recursive) {
                        Object value = null;

                        try {
                            value = ctx.lookup(name);
                        }
                        catch (Throwable t) {
                            appendErrorTag(buffer,
                                    "Failed to lookup: " + name +
                                            ", errmsg=" + t.getMessage());
                        }

                        if (value instanceof Context) {
                            Context subctx = (Context) value;
                            openContextTag(buffer);
                            appendNCPTag(buffer, pair);

                            try {
                                listXML(subctx, buffer);
                            }
                            catch (Throwable t) {
                                appendErrorTag(buffer,
                                        "Failed to list contents of: " + name +
                                                ", errmsg=" + t.getMessage());
                            }

                            closeContextTag(buffer);
                        } else {
                            appendNonContextTag(buffer, pair);
                        }
                    } else {
                        appendLeafTag(buffer, pair);
                    }
                }
            }
            ne.close();
        }
        catch (NamingException ne) {
            appendErrorTag(buffer,
                    "error while listing context " +
                            ctx.toString() + ": " + ne.toString(true));
        }
    }

    private void openJndiTag(StringBuffer buffer) {
        buffer.append("<jndi>\n");
    }

    private void closeJndiTag(StringBuffer buffer) {
        buffer.append("</jndi>\n");
    }

    private void appendPreExceptionTag(StringBuffer buffer,
                                       String msg,
                                       Throwable t) {
        buffer.append("<pre>\n").append(msg).append("\n");
        formatException(buffer, t);
        buffer.append("</pre>\n");
    }

    private void appendBeanTag(StringBuffer buffer, String bean) {
        buffer.append("<name>java:comp</name>\n");
        buffer.append("<attribute name='bean'>").append(bean).append("</attribute>\n");
    }

    private void appendJavaNameTag(StringBuffer buffer) {
        buffer.append("<name>java:</name>\n");
    }

    private void appendGlobalNameTag(StringBuffer buffer) {
        buffer.append("<name>Global</name>\n");
    }


    private void appendLinkRefTag(StringBuffer buffer,
                                  LinkRef link,
                                  NameClassPair ncp) {
        buffer.append("<link-ref>\n");
        buffer.append("<name>").append(ncp.getName()).append("</name>\n");
        try {
            String lName = link.getLinkName();
            buffer.append("<link>").append(lName).append("</link>\n");
        }
        catch (NamingException e) {
            appendErrorTag(buffer,
                    "Failed to getLinkName, " + e.toString(true));
        }
        buffer.append("<attribute name='class'>").append(ncp.getClassName()).append(
                "</attribute>\n");
        buffer.append("</link-ref>\n");
    }

    private void appendLinkRefErrorTag(StringBuffer buffer) {
        buffer.append("<link-ref>\n");
        buffer.append("<name>Invalid</name>\n");
        buffer.append("</link-ref>\n");
    }

    private void openContextTag(StringBuffer buffer) {
        buffer.append("<context>\n");
    }

    private void closeContextTag(StringBuffer buffer) {
        buffer.append("</context>\n");
    }

    private void appendNonContextTag(StringBuffer buffer, NameClassPair ncp) {
        buffer.append("<non-context>\n");
        appendNCPTag(buffer, ncp);
        buffer.append("</non-context>\n");
    }

    private void appendLeafTag(StringBuffer buffer, NameClassPair ncp) {
        buffer.append("<leaf>\n");
        appendNCPTag(buffer, ncp);
        buffer.append("</leaf>\n");
    }

    private void appendNCPTag(StringBuffer buffer, NameClassPair ncp) {
        buffer.append("<name>").append(ncp.getName()).append("</name>\n");
        buffer.append("<attribute name='class'>").append(ncp.getClassName()).append(
                "</attribute>\n");
    }

    private void appendErrorTag(StringBuffer buffer, String msg) {
        buffer.append("<error>\n");
        buffer.append("<message>").append(msg).append("</message>\n");
        buffer.append("</error>\n");
    }

    private void formatException(StringBuffer buffer, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        buffer.append("<pre>\n");
        t.printStackTrace(pw);
        buffer.append(sw.toString());
        buffer.append("</pre>\n");
    }

    private MBeanServer getMbeanServer() {
        MBeanServer mbeanServer = mbeanServerValue.getOptionalValue();
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }
}
