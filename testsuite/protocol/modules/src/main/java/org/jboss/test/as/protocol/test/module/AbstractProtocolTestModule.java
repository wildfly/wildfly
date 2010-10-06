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
package org.jboss.test.as.protocol.test.module;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.jboss.as.server.manager.ServerManagerEnvironment;
import org.jboss.test.as.protocol.support.server.ServerNoopExiter;
import org.jboss.test.as.protocol.support.server.manager.ServerManagerNoopExiter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class AbstractProtocolTestModule {

    static {
        org.jboss.as.server.manager.SystemExiter.initialize(new ServerManagerNoopExiter());
        org.jboss.as.server.SystemExiter.initialize(new ServerNoopExiter());
    }

    Set<String> setProperties = new HashSet<String>();

    protected void addProperty(String name, String value) {
        System.setProperty(name, value);
        setProperties.add(name);
    }

    protected void setDomainConfigDir(String name) throws URISyntaxException {
        addProperty(ServerManagerEnvironment.DOMAIN_CONFIG_DIR, findDomainConfigsDir(name));
    }

    public void beforeTest() throws Exception {
    }

    public void afterTest() throws Exception {
        for (String name : setProperties)
            System.getProperties().remove(name);
    }

    protected String findDomainConfigsDir(String name) throws URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("org/jboss/as/server/manager/domainconfigs/marker");
        if (url == null)
            throw new IllegalStateException("Could not find domainconfigs directory");
        File file = new File(url.toURI());
        file = file.getParentFile();
        return new File(file, name).getAbsolutePath();
    }


    protected boolean managerAlive(InetAddress addr, int port) {
        Socket socket = null;
        try {
            socket = new Socket(addr, port);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }


    protected void waitForManagerToStop(InetAddress addr, int port, int timeoutMillis) throws Exception {
        long end = System.currentTimeMillis() + timeoutMillis;
        do {
            if (!managerAlive(addr, port))
                return;
            Thread.sleep(300);
        }while (System.currentTimeMillis() < end);

        Assert.fail("ServerManager did not stop in " + timeoutMillis + "ms");
    }
}
