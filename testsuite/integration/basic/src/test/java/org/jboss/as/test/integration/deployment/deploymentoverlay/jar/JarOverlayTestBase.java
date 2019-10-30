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

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.test.integration.deployment.deploymentoverlay.AbstractOverlayTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author baranowb
 *
 */
public class JarOverlayTestBase extends AbstractOverlayTestBase {

    public static Archive<?> createOverlayedArchive(final boolean resourcePresent, final String deploymentOverlayedArchive){
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, deploymentOverlayedArchive);
        jar.addClasses(OverlayableInterface.class, OverlayEJB.class);
        jar.addAsManifestResource(new StringAsset(OverlayableInterface.STATIC), OverlayableInterface.RESOURCE_STATIC_META_INF);

        if(resourcePresent){
            jar.addAsManifestResource(new StringAsset(OverlayableInterface.ORIGINAL), OverlayableInterface.RESOURCE_META_INF);
        }
        return jar;
    }

    protected static InitialContext getInitialContext() throws NamingException {
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + 8080);
        return new InitialContext(env);
    }

    protected static String getEjbBinding(final String rtModuleName, final String module, final String distinct, final Class bean,
            final Class iface) {

        final String appName = rtModuleName;
        final String moduleName = module;
        final String distinctName = distinct;
        final String beanName = bean.getSimpleName();
        final String viewClassName = iface.getName();
        return "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName;
    }
}