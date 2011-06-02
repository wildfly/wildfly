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
package org.jboss.as.demos.rar.mbean;

import javax.naming.InitialContext;

import org.jboss.as.demos.rar.archive.HelloWorldConnection;
import org.jboss.as.demos.rar.archive.HelloWorldConnectionFactory;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class Test implements TestMBean {

    private static final String JNDI_NAME = "java:/eis/HelloWorld";
    @Override
    public String helloWorld() throws Exception {
        return getConnection().helloWorld();
    }

    @Override
    public String helloWorld(String name) throws Exception {
        return getConnection().helloWorld(name);
    }

    private HelloWorldConnection getConnection() throws Exception {
        InitialContext context = new InitialContext();
        HelloWorldConnectionFactory factory = (HelloWorldConnectionFactory)context.lookup(JNDI_NAME);
        HelloWorldConnection conn = factory.getConnection();
        if (conn == null) {
            throw new RuntimeException("No connection");
        }
        return conn;
    }
}
