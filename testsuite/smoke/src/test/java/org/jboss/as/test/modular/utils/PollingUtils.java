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
package org.jboss.as.test.modular.utils;

import static org.jboss.as.protocol.StreamUtils.safeClose;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Since things get installed asynchronously, a deployment is sometimes
 * reported as installed before we can actually use it. Work around this
 * by retrying for now.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class PollingUtils {

    public static void retryWithTimeout(long timeoutMs, Task task) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        do {
            if (task.execute()) {
                return;
            }
            Thread.sleep(100);
        } while (System.currentTimeMillis() < end);

        throw new RuntimeException("Task could not be completed within " + timeoutMs + "ms");
    }

    public interface Task {
        boolean execute() throws Exception;
    }

    public static class WaitForMBeanTask implements Task {
        private final MBeanServer server;
        private final ObjectName name;

        public WaitForMBeanTask(MBeanServer server, ObjectName name) {
            this.server = server;
            this.name = name;
        }

        @Override
        public boolean execute() throws Exception {
            try {
                return server.getMBeanInfo(name) != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class JndiLookupTask implements Task {
        private final Context context;
        private final String name;
        private Object result;

        public JndiLookupTask(Context context, String name) {
            this.context = context;
            this.name = name;
        }

        @Override
        public boolean execute() throws Exception {
            try {
                result = context.lookup(name);
            } catch (NamingException e) {
                return false;
            }
            return true;
        }

        public <T> T getResult(Class<T> clazz) {
            return clazz.cast(result);
        }
    }

    public static class UrlConnectionTask implements Task {
        private final URL url;
        private String response;

        public UrlConnectionTask(URL url) {
            this.url = url;
        }

        @Override
        public boolean execute() throws Exception {
            URLConnection conn = null;
            InputStream in = null;
            try {
                System.out.println("Reading response from " + url + ":");
                conn = url.openConnection();
                conn.setDoInput(true);
                in = new BufferedInputStream(conn.getInputStream());
                int i = in.read();
                StringBuilder sb = new StringBuilder();
                while (i != -1) {
                    sb.append((char) i);
                    i = in.read();
                }
                response = sb.toString();
                return true;
            } catch (Exception e) {
                return false;
            }finally {
                safeClose(in);
            }
        }

        public String getResponse() {
            return response;
        }
    }
}
