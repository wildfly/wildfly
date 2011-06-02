/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.ejb3.runner;

import java.util.concurrent.Future;
import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.ejb3.archive.AsyncLocal;
import org.jboss.as.demos.ejb3.archive.SimpleSingletonLocal;
import org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionBean;
import org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionLocal;
import org.jboss.as.demos.ejb3.mbean.ExerciseBMT;
import org.jboss.as.demos.ejb3.mbean.ExerciseEchoService;
import org.jboss.as.demos.ejb3.mbean.ExercisePatClifton;
import org.jboss.as.demos.ejb3.mbean.ExerciseStateful;
import org.jboss.as.demos.ejb3.mbean.Test;
import org.jboss.as.demos.ejb3.mdb.PostmanPatMDB;
import org.jboss.as.demos.ejb3.rar.SimpleQueueResourceAdapter;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

import static org.jboss.as.protocol.StreamUtils.safeClose;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class ExampleRunner {
    private static <T> T createProxy(MBeanServerConnection mbeanServer, String lookupName, Class<T> intf) {
        final InvocationHandler handler = new TestMBeanInvocationHandler(mbeanServer, lookupName);
        final Class<?>[] interfaces = {intf};
        return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), interfaces, handler));
    }

    private static void exec(MBeanServerConnection server, Class<? extends Callable<?>> callableClass) throws Exception {
        String msg = (String) server.invoke(new ObjectName("jboss:name=ejb3-test,type=service"), "exec", new Object[]{callableClass}, new String[]{Class.class.getName()});
        System.out.println(msg);
    }

    private static void workOnSingletoBean(MBeanServerConnection server, String jndiName, int numThreads, int numTimes) throws Exception {
        int singletonBeanInstances = (Integer) server.invoke(new ObjectName("jboss:name=ejb3-test,type=service"), "lookupSingleton", new Object[]{jndiName, numThreads, numTimes}, new String[]{String.class.getName(), Integer.TYPE.getName(), Integer.TYPE.getName()});
        System.out.println("Number of singleton bean instances created is: " + singletonBeanInstances);


        int count = (Integer) server.invoke(new ObjectName("jboss:name=ejb3-test,type=service"), "invokeSingleton", new Object[]{jndiName, numThreads, numTimes}, new String[]{String.class.getName(), Integer.TYPE.getName(), Integer.TYPE.getName()});
        System.out.println("Count is: " + count);
    }

    public static void main(String[] args) throws Exception {
        DeploymentUtils utils = new DeploymentUtils();
        try {
            utils.addDeployment("ejb3-rar.rar", SimpleQueueResourceAdapter.class.getPackage());
            utils.addDeployment("ejb3-mdb.jar", PostmanPatMDB.class.getPackage());
            utils.addDeployment("ejb3-example.jar", SimpleStatelessSessionBean.class.getPackage());
            utils.addDeployment("ejb3-mbean.sar", Test.class.getPackage());
            utils.deploy();

            MBeanServerConnection mbeanServer = utils.getConnection();

            SimpleStatelessSessionLocal bean = createProxy(mbeanServer, "java:global/ejb3-example/SimpleStatelessSessionBean!" + SimpleStatelessSessionLocal.class.getName(), SimpleStatelessSessionLocal.class);
            String msg = bean.echo("Hello world");
            System.out.println(msg);

            final String result = (String)mbeanServer.invoke(new ObjectName("jboss:name=ejb3-test,type=service"), "callAsync", new Object[] {"java:global/ejb3-example/AsyncBean!" + AsyncLocal.class.getName(), "Hello World"}, new String[] {String.class.getName(), String.class.getName()});
            System.out.println(result);

            exec(mbeanServer, ExerciseStateful.class);

            String singletonBeanJndiName = "java:global/ejb3-example/SimpleSingletonBean!" + SimpleSingletonLocal.class.getName();
            workOnSingletoBean(mbeanServer, singletonBeanJndiName, 100, 10);

            exec(mbeanServer, ExerciseBMT.class);

            exec(mbeanServer, ExerciseEchoService.class);

            exec(mbeanServer, ExercisePatClifton.class);
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }
}
