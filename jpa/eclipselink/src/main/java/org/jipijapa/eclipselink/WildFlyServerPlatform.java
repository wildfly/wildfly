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

import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.platform.server.jboss.JBossPlatform;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.transaction.jboss.JBossTransactionController;

/**
 * The fully qualified name of WildFlyServerPlatform must be set as the value of
 * the eclipselink.target-server property on EclipseLink version 2.3.2 and
 * older. In newer versions where bug
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=365704 has been fixed, setting
 * eclipselink.target-server to "jboss" is sufficient.
 *
 * @author Craig Ringer <ringerc@ringerc.id.au>
 *
 */
public class WildFlyServerPlatform extends JBossPlatform {

    /**
     * This JNDI address is for JMX MBean registration. Copy pasted from: <br>
     * {@link org.eclipse.persistence.platform.server.JMXServerPlatformBase} .
     */
    private static final String JMX_JNDI_RUNTIME_REGISTER = "java:comp/env/jmx/runtime";

    /**
     * During deployment, when the first entity manager is created, the
     * application deployment becomes extremelty slow due to the
     * MbeanServer.getbeanCount() access done by eclipselink. This access is on
     * top of all things done only for logging purposes. We completely kill the
     * call to the api.
     */
    private static final int MBEAN_SERVER_COUNT_DUMMY_VALUE = -999;

    public WildFlyServerPlatform(DatabaseSession newDatabaseSession) {
        super(newDatabaseSession);
    }

    @Override
    public Class<?> getExternalTransactionControllerClass() {
        return JBossAS7TransactionController.class;
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

    /**
     * We are forced to override the code of parent class due to fact that the
     * parent code is extremelly slow when it tries to create log statements
     * that cound the number of managed beans in the MBeanServer. In this
     * override, we remove all such expensive calls by a simple use of dummy
     * constant.
     *
     * <P>
     * REFERNECES: <br>
     * <a href="https://issues.jboss.org/browse/WFLY-9408">Wildfly 10 - The cost
     * of creating the first eclipselink EntityManager during deployment is
     * extremelly high due to MBeanServer.getMbeanCount() cost</a>
     */
    @Override
    public MBeanServer getMBeanServer() {
        /**
         * This function will attempt to get the MBeanServer via the
         * findMBeanServer spec call. 1) If the return list is null we attempt
         * to retrieve the PlatformMBeanServer (if it exists or is enabled in
         * this security context). 2) If the list of MBeanServers returned is
         * more than one we get the lowest indexed MBeanServer that does not on
         * a null default domain. 3) 333336: we need to wrap JMX calls in
         * doPrivileged blocks 4) fail-fast: if there are any issues with JMX -
         * continue - don't block the deploy()
         */
        // lazy initialize the MBeanServer reference
        if (null == mBeanServer) {
            List<MBeanServer> mBeanServerList = null;
            try {
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                    try {
                        mBeanServerList = (List<MBeanServer>) AccessController
                                .doPrivileged(new PrivilegedExceptionAction() {
                                    @Override
                                    public List<MBeanServer> run() {
                                        return MBeanServerFactory.findMBeanServer(null);
                                    }
                                });
                    } catch (PrivilegedActionException pae) {
                        getAbstractSession().log(SessionLog.WARNING, SessionLog.SERVER, "failed_to_find_mbean_server",
                                "null or empty List returned from privileged MBeanServerFactory.findMBeanServer(null)");
                        Context initialContext = null;
                        try {
                            initialContext = new InitialContext(); // the
                                                                   // context
                                                                   // should be
                                                                   // cached
                            mBeanServer = (MBeanServer) initialContext.lookup(JMX_JNDI_RUNTIME_REGISTER);
                        } catch (NamingException ne) {
                            getAbstractSession().log(SessionLog.WARNING, SessionLog.SERVER,
                                    "failed_to_find_mbean_server", ne);
                        }
                    }
                } else {
                    mBeanServerList = MBeanServerFactory.findMBeanServer(null);
                }
                // Attempt to get the first MBeanServer we find - usually there
                // is only one - when agentId == null we return a List of them
                if (null == mBeanServer && (null == mBeanServerList || mBeanServerList.isEmpty())) {
                    // Unable to acquire a JMX specification List of MBeanServer
                    // instances
                    getAbstractSession().log(SessionLog.WARNING, SessionLog.SERVER, "failed_to_find_mbean_server",
                            "null or empty List returned from MBeanServerFactory.findMBeanServer(null)");
                    // Try alternate static method
                    if (!PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                        mBeanServer = ManagementFactory.getPlatformMBeanServer();
                        if (null == mBeanServer) {
                            getAbstractSession().log(SessionLog.WARNING, SessionLog.SERVER,
                                    "failed_to_find_mbean_server",
                                    "null returned from ManagementFactory.getPlatformMBeanServer()");
                        } else {
                            getAbstractSession().log(SessionLog.FINER, SessionLog.SERVER,
                                    "jmx_mbean_runtime_services_registration_mbeanserver_print",
                                    new Object[] { mBeanServer, MBEAN_SERVER_COUNT_DUMMY_VALUE,
                                            mBeanServer.getDefaultDomain(), 0 });
                        }
                    }
                } else {
                    // Use the first MBeanServer by default - there may be
                    // multiple domains each with their own MBeanServer
                    mBeanServer = mBeanServerList.get(JMX_MBEANSERVER_INDEX_DEFAULT_FOR_MULTIPLE_SERVERS);
                    if (mBeanServerList.size() > 1) {
                        // There are multiple MBeanServerInstances (usually only
                        // JBoss)
                        // 328006: WebLogic may also return multiple instances
                        // (we need to register the one containing the com.bea
                        // tree)
                        getAbstractSession().log(SessionLog.WARNING, SessionLog.SERVER,
                                "jmx_mbean_runtime_services_registration_encountered_multiple_mbeanserver_instances",
                                mBeanServerList.size(), JMX_MBEANSERVER_INDEX_DEFAULT_FOR_MULTIPLE_SERVERS,
                                mBeanServer);
                        // IE: for JBoss we need to verify we are using the
                        // correct MBean server of the two (default, null)
                        // Check the domain if it is non-null - avoid using this
                        // server
                        int index = 0;
                        for (MBeanServer anMBeanServer : mBeanServerList) {
                            getAbstractSession().log(SessionLog.FINER, SessionLog.SERVER,
                                    "jmx_mbean_runtime_services_registration_mbeanserver_print",
                                    new Object[] { anMBeanServer, MBEAN_SERVER_COUNT_DUMMY_VALUE,
                                            anMBeanServer.getDefaultDomain(), index });
                            if (null != anMBeanServer.getDefaultDomain()) {
                                mBeanServer = anMBeanServer;
                                getAbstractSession().log(SessionLog.WARNING, SessionLog.SERVER,
                                        "jmx_mbean_runtime_services_switching_to_alternate_mbeanserver", mBeanServer,
                                        index);
                            }
                            index++;
                        }
                    } else {
                        // Only a single MBeanServer instance was found
                        // mBeanServer.getMBeanCount() - This is very slow on
                        // wildfly
                        // we are disabling this statemnt
                        getAbstractSession().log(SessionLog.FINER, SessionLog.SERVER,
                                "jmx_mbean_runtime_services_registration_mbeanserver_print", new Object[] { mBeanServer,
                                        MBEAN_SERVER_COUNT_DUMMY_VALUE, mBeanServer.getDefaultDomain(), 0 });
                    }
                }
            } catch (Exception e) {
                // TODO: Warning required
                e.printStackTrace();
            }
        }
        return mBeanServer;
    }

}
