/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.client;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.arquillian.container.NetworkUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.ORBPackage.InvalidName;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jts.ORBManager;
import com.arjuna.ats.internal.jts.context.ContextPropagationManager;
import com.arjuna.ats.jts.OTSManager;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import com.sun.corba.se.impl.orbutil.ORBConstants;

public class Util {
    public static final String HOST = NetworkUtils.formatPossibleIpv6Address(System.getProperty("node1", "localhost"));
    private static org.omg.CORBA.ORB orb = null;
    private static ORB arjunaORB = null;

    // Recovery manager is needed till the end of orb usage
    private static ExecutorService recoveryManagerPool;

    public static void presetOrb() throws InvalidName, SystemException {
        Properties properties = new Properties();
        properties.setProperty(ORBConstants.SERVER_HOST_PROPERTY, TestSuiteEnvironment.getServerAddress());
        properties.setProperty(ORBConstants.PERSISTENT_SERVER_PORT_PROPERTY, "15151");
        properties.setProperty(ORBConstants.ORB_SERVER_ID_PROPERTY, "1");

        new ContextPropagationManager();

        orb = org.omg.CORBA.ORB.init(new String[0], properties);

        arjunaORB = com.arjuna.orbportability.ORB.getInstance("ClientSide");
        arjunaORB.setOrb(orb);

        OA oa = OA.getRootOA(arjunaORB);
        org.omg.PortableServer.POA rootPOA = org.omg.PortableServer.POAHelper.narrow(orb
                .resolve_initial_references("RootPOA"));
        oa.setPOA(rootPOA);

        oa.initOA();

        ORBManager.setORB(arjunaORB);
        ORBManager.setPOA(oa);

        // Recovery manager has to be started on client when we want recovery
        // and we start the transaction on client
        recoveryManagerPool = Executors.newFixedThreadPool(1);
        recoveryManagerPool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                RecoveryManager.main(new String[] { "-test" });
                return "Running recovery manager";
            }
        });

    }

    public static void tearDownOrb() {
        ORBManager.reset();
        if (recoveryManagerPool != null) {
            recoveryManagerPool.shutdown();
        }
    }

    public static void startCorbaTx() throws Throwable {
        OTSManager.get_current().begin();
    }

    public static void commitCorbaTx() throws Throwable {
        OTSManager.get_current().commit(true);
    }

    public static void rollbackCorbaTx() throws Throwable {
        OTSManager.get_current().rollback();
    }

    public static InitialContext getContext() throws NamingException {
        // this is needed to get the iiop call successful
        System.setProperty("com.sun.CORBA.ORBUseDynamicStub", "true");

        final Properties prope = new Properties();
        prope.put(Context.PROVIDER_URL, "corbaloc::" + HOST + ":3628/JBoss/Naming/root");
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put("java.naming.corba.orb", orb);

        return new InitialContext(prope);
    }
}
