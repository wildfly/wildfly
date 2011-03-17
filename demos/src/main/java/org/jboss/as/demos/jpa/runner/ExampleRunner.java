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
package org.jboss.as.demos.jpa.runner;

import org.jboss.as.demos.DeploymentUtils;
import org.jboss.as.demos.jpa.archive.SimpleStatelessSessionBean;
import org.jboss.as.demos.jpa.archive.SimpleStatelessSessionLocal;
import org.jboss.as.demos.jpa.mbean.ExerciseStateful;
import org.jboss.as.demos.jpa.mbean.Test;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

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

    private static void doStatefulMagic(MBeanServerConnection server) throws Exception {
        String msg = (String) server.invoke(new ObjectName("jboss:name=jpa-test,type=service"), "exec", new Object[]{ExerciseStateful.class}, new String[]{Class.class.getName()});
        System.out.println(msg);

        msg = (String) server.invoke(new ObjectName("jboss:name=jpa-test,type=service"), "exec", new Object[]{ExerciseStateful.class}, new String[]{Class.class.getName()});
        System.out.println(msg);

    }

    public static void main(String[] args) throws Exception {
        showInfo();
        DeploymentUtils utils = new DeploymentUtils("jpa-example.jar", SimpleStatelessSessionBean.class.getPackage());
        try {
            utils.addDeployment("jpa-mbean.sar", Test.class.getPackage());
            utils.deploy();

            /*
            InitialContext ctx = new InitialContext();
            SimpleStatelessSessionLocal bean = (SimpleStatelessSessionLocal) ctx.lookup("java:global/jpa-example/SimpleStatelessSessionBean!" + SimpleStatelessSessionLocal.class.getName());
            String msg = bean.echo("Hello world");
            */
            MBeanServerConnection mbeanServer = utils.getConnection();

            SimpleStatelessSessionLocal bean = createProxy(mbeanServer, "java:global/jpa-example/SimpleStatelessSessionBean!" + SimpleStatelessSessionLocal.class.getName(), SimpleStatelessSessionLocal.class);
            String msg = bean.echo("Hello world");
            System.out.println(msg);

            doStatefulMagic(mbeanServer);
        } finally {
            utils.undeploy();
            safeClose(utils);
        }
    }

    private static void showInfo() {
        System.out.println("Thanks for running the JPA demo and helping to test the bits out on your system.  By the way, "+
            "its normal to see a 'RejectedExecutionException' at the end of the demo but other errors" +
            " probably mean a problem occurred.");
    }
}
