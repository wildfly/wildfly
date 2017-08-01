/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.exception;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * The same bundle of tests as runs at {@link ExceptionTestCase} but these ones
 * are managed at client mode - all calls runs over ejb remoting.
 *
 *  @author Ondrej Chaloupka
 */
@RunAsClient
@RunWith(Arquillian.class)
public class ExceptionEjbClientTestCase extends ExceptionTestCase {
    private static Context context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + 8080);
        context = new InitialContext(props);
    }

    private <T> T lookup(Class<? extends T> beanType, Class<T> interfaceType,boolean isStateful) throws NamingException {
        String ejbLookup = String.format("ejb:/%s/%s!%s%s", ARCHIVE_NAME, beanType.getSimpleName(), interfaceType.getName(),
                (isStateful ? "?stateful" : ""));
        return interfaceType.cast(context.lookup(ejbLookup));
    }

    protected SFSB1Interface getBean() throws NamingException {
        return lookup(SFSB1.class, SFSB1Interface.class, true);
    }

    protected DestroyMarkerBeanInterface getMarker() throws NamingException {
        return lookup(DestroyMarkerBean.class, DestroyMarkerBeanInterface.class, false);
    }
}
