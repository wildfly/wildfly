/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
