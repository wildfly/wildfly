/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jpa.hibernate3;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.transaction.JNDITransactionManagerLookup;
import org.jboss.as.jpa.spi.JtaManager;


/**
 * Hibernate 3.6.x integration
 *
 * @author Scott Marlow
 */
public class JBossAppServerJtaPlatform extends JNDITransactionManagerLookup {

    private static volatile JtaManager jtaManager;

    public static void initJBossAppServerJtaPlatform(final JtaManager manager) {
        jtaManager = manager;
    }

    @Override
    public TransactionManager getTransactionManager(Properties props) throws HibernateException {
        return jtaManager.locateTransactionManager();
    }

    @Override
    protected String getName() {
        return "java:jboss/TransactionManager";
    }

    @Override
    public String getUserTransactionName() {
        return "java:comp/UserTransaction";
    }
}
