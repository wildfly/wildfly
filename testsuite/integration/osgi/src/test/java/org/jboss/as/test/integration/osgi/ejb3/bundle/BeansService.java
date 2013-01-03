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
package org.jboss.as.test.integration.osgi.ejb3.bundle;

import java.util.concurrent.Callable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.test.integration.osgi.api.Echo;
import org.junit.Assert;
import org.osgi.framework.BundleContext;

/**
 * @author thomas.diesler@jboss.com
 * @since 28-Sep-2012
 */
public class BeansService implements Callable<Boolean> {

    private static final String JAVA_GLOBAL_NAMESPACE_PREFIX = "java:global/";
    //private static final String JAVA_APP_NAMESPACE_PREFIX = "java:app/";
    //private static final String JAVA_MODULE_NAMESPACE_PREFIX = "java:module/";

    private final BundleContext context;

    BeansService(BundleContext context) {
        this.context = context;
    }

    @Override
    public Boolean call() throws Exception {
        String moduleName = context.getBundle().getSymbolicName();
        moduleName = moduleName.substring(0, moduleName.indexOf(".jar"));
        String ejbName = SampleSLSB.class.getSimpleName();
        verifyBindings(moduleName, ejbName, RemoteEcho.class.getName());
        verifyBindings(moduleName, ejbName, Echo.class.getName());
        verifyBindings(moduleName, ejbName, SampleSLSB.class.getName());

        ejbName = SampleSFSB.class.getSimpleName();
        verifyBindings(moduleName, ejbName, RemoteEcho.class.getName());
        verifyBindings(moduleName, ejbName, Echo.class.getName());
        verifyBindings(moduleName, ejbName, SampleSFSB.class.getName());
        return true;
    }

    private void verifyBindings(String moduleName, String ejbName, String typeName) throws NamingException {
        Context ctx = new InitialContext();

        String lookup = JAVA_GLOBAL_NAMESPACE_PREFIX + moduleName + "/" + ejbName + "!" + typeName;
        Echo localBusinessInterface = (Echo) ctx.lookup(lookup);
        Assert.assertNotNull("Not null: " + lookup, localBusinessInterface);

        //lookup = JAVA_APP_NAMESPACE_PREFIX + moduleName + "/" + ejbName + "!" + typeName;
        //Echo localBusinessInterfaceInAppNamespace = (Echo) ctx.lookup(lookup);
        //Assert.assertNotNull("Not null: " + lookup, localBusinessInterfaceInAppNamespace);

        //lookup = JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + typeName;
        //Echo localBusinessInterfaceInModuleNamespace = (Echo) ctx.lookup(lookup);
        //Assert.assertNotNull("Not null: " + lookup, localBusinessInterfaceInModuleNamespace);
    }
}