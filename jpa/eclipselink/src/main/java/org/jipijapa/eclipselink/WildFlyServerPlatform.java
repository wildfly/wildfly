/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jipijapa.eclipselink;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.platform.server.jboss.JBossPlatform;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.transaction.jboss.JBossTransactionController;
import org.jipijapa.JipiLogger;

/**
 * The fully qualified name of WildFlyServerPlatform must be set as the value
 * of the eclipselink.target-server property on EclipseLink version 2.3.2 and
 * older. In newer versions where bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=365704
 * has been fixed, setting eclipselink.target-server to "jboss" is sufficient.
 *
 * @author Craig Ringer <ringerc@ringerc.id.au>
 *
 */
public class WildFlyServerPlatform extends JBossPlatform {

    public WildFlyServerPlatform(DatabaseSession newDatabaseSession) {
        super(newDatabaseSession);
    }

    @Override
    public Class<?> getExternalTransactionControllerClass() {
        return JBossAS7TransactionController.class;
    }

    @Override
    public MBeanServer getMBeanServer() {
        if(mBeanServer == null) {
            List<MBeanServer> mBeanServerList = null;
            try {
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                    try {
                        mBeanServerList = AccessController.doPrivileged(
                                new PrivilegedExceptionAction<List<MBeanServer>>() {
                                    public List<MBeanServer> run() {
                                        return MBeanServerFactory.findMBeanServer(null);
                                    }
                                }
                        );
                    } catch (PrivilegedActionException pae) {
                        // Skip the superclass impl of a JNDI lookup
                    }
                } else {
                    mBeanServerList = MBeanServerFactory.findMBeanServer(null);
                }
                if (mBeanServer == null) {
                    // Attempt to get the first MBeanServer we find - usually there is only one - when agentId == null we return a List of them
                    if (mBeanServerList != null &&  !mBeanServerList.isEmpty()) {
                        // Use the first MBeanServer by default - there may be multiple domains each with their own MBeanServer
                        mBeanServer = mBeanServerList.get(JMX_MBEANSERVER_INDEX_DEFAULT_FOR_MULTIPLE_SERVERS);
                        if (mBeanServerList.size() > 1) {
                            if (null != mBeanServer.getDefaultDomain()) {
                                // Prefer no default domain, as WildFly does not register an mbean server with a default domain
                                for (int i = 1; i < mBeanServerList.size(); i++) {
                                    MBeanServer anMBeanServer = mBeanServerList.get(i);
                                    if (null == anMBeanServer.getDefaultDomain()) {
                                        mBeanServer = anMBeanServer;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // else {
                    // Skip the superclass impl of trying ManagementFactory.getPlatformMBeanServer()
                    // if privileged access is disabled.
                    // WildFly has already called that by the time this code would get run, so if we
                    // got here it's an error situation and we should just return null
                    // }
                }
            } catch (Exception e) {
                JipiLogger.JPA_LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
        return mBeanServer;
    }

    public static class JBossAS7TransactionController extends JBossTransactionController {

        private static final String JBOSS_TRANSACTION_MANAGER = "java:jboss/TransactionManager";

        @Override
        protected TransactionManager acquireTransactionManager() throws Exception {
            try {
                return InitialContext.doLookup(JBOSS_TRANSACTION_MANAGER);
            } catch (NamingException ex) {
                return super.acquireTransactionManager();
            }
        }
    }

}
