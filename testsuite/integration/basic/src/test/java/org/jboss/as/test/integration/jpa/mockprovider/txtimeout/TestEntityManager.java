/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.tm.TxUtils;

/**
 * TestEntityManager
 *
 * @author Scott Marlow
 */
public class TestEntityManager implements InvocationHandler {

    private static AtomicBoolean closedByReaperThread = new AtomicBoolean(false);
    private static final List<String> invocations = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        invocations.add(method.getName());

        if (method.getName().equals("persist")) {
            return persist();
        }

        if (method.getName().equals("close")) {
            return close();
        }

        if (method.getName().equals("toString")) {
            return "EntityManager";
        }

        return null;

    }

    private Object persist() {
        DataSource dataSource = null;
        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("naming error creating initial context");
        }
        try {
            dataSource = (DataSource) context.lookup("java:jboss/datasources/ExampleDS");
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("naming error creating initial context", e);
        }
        try {
            dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("datasource error getting connection", e);
        }

        return null;
    }

    public static boolean getClosedByReaperThread() {
        return closedByReaperThread.get();
    }

    private Object close() {
        boolean isBackgroundReaperThread =
                TxUtils.isTransactionManagerTimeoutThread();
        if (isBackgroundReaperThread) {
            closedByReaperThread.set(true);
        } else {
            closedByReaperThread.set(false);
        }
        return null;
    }

    public static void clearState() {
        closedByReaperThread.set(false);
    }
}
