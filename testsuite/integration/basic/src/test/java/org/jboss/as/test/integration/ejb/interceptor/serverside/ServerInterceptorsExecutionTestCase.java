/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import static org.jboss.as.test.integration.ejb.interceptor.serverside.ServerInterceptorsTestCase.getArchive;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerInterceptorsExecutionTestCase {

    @Deployment
    public static Archive<?> deploy() throws Exception {
        return getArchive();
    }

    @Test
    public void serverInterceptorExecutionCheck() throws NamingException {
        final InitialContext ctx = new InitialContext();
        SampleBean bean = (SampleBean) ctx.lookup("java:module/" + SampleBean.class.getSimpleName());

        try {
            Executors.newFixedThreadPool(1).submit(new Callable<String>() {
                @Override
                public String call() {
                    // call bean method in order to execute interceptor's method
                    return bean.getSimpleName();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            // waiting for the interceptor's method execution
            ServerInterceptor.latch.await();
        }
        catch (InterruptedException ie){
            throw new RuntimeException("latch.await() has been interrupted", ie);
        }
    }
}
