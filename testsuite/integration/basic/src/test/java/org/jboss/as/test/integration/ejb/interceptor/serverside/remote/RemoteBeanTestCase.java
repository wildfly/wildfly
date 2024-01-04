/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.remote;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case verifying server-side interceptor execution while accessing a stateful EJB implementing @Remote interface.
 *
 * @author Sultan Zhantemirov
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteBeanTestCase.SetupTask.class)
public class RemoteBeanTestCase {

    private static Context ctx;
    private static final String MODULE_NAME = "remote-stateful-ejb";

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        ctx = new InitialContext(props);
    }

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(RemoteBeanTestCase.class.getPackage());
        jar.addPackage(AbstractServerInterceptorsSetupTask.class.getPackage());
        return jar;
    }

    @Test
    @RunAsClient
    public void remoteBeanCheck() throws Exception {
        final SimpleCounter bean = (SimpleCounter) ctx.lookup("ejb:" + "" + "/" + MODULE_NAME + "/" + ""
                + "/" + StatefulCounterBean.class.getSimpleName() + "!" + SimpleCounter.class.getName() + "?stateful");
        Assert.assertNotNull("Null bean proxy has been returned by context lookup", bean);

        for (int i = 0; i < 5; i++) {
            bean.increment();
        }

        Assert.assertEquals(5, bean.getCount());
        Assert.assertEquals(LoggingInterceptor.PREFIX + StatefulCounterBean.class.getSimpleName(), bean.getSimpleName());
    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    LoggingInterceptor.class,
                    "interceptor-module-remote",
                    "module.xml",
                    RemoteBeanTestCase.class.getResource("module.xml"),
                    "server-side-interceptor-remote.jar"
                    )
            );
        }
    }
}
