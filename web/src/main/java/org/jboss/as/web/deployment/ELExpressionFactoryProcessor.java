/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import javax.el.BeanELResolver;
import javax.el.ExpressionFactory;

import org.apache.el.ExpressionFactoryImpl;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.as.web.WebLogger;
import org.jboss.el.cache.FactoryFinderCache;
import org.jboss.modules.Module;

/**
 * Processor that handles {@link javax.el.ExpressionFactory} instances that are found in the deployment.
 * This resolves a performance issue.
 *
 * @author Stuart Douglas
 * @see <a href="https://issues.jboss.org/browse/AS7-5026">jira</a>
 */
public class ELExpressionFactoryProcessor implements DeploymentUnitProcessor {

    public static final String FACTORY_ID = ExpressionFactory.class.getName();
    private static final Method purge;

    static {
        try {
            purge = BeanELResolver.class.getDeclaredMethod("purgeBeanClasses", ClassLoader.class);
            purge.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null) {
            return;
        }
        final ServicesAttachment services = deploymentUnit.getAttachment(Attachments.SERVICES);
        final List<String> implementations = services.getServiceImplementations(ExpressionFactory.class.getName());
        if (implementations != null && implementations.size() > 0) {
            FactoryFinderCache.addCacheEntry(module.getClassLoader(), FACTORY_ID, implementations.get(0));
        }


        // try to read from $java.home/lib/el.properties
        try {
            String javah = System.getProperty("java.home");
            String configFile = javah + File.separator +
                    "lib" + File.separator + "el.properties";
            File f = new File(configFile);
            if (f.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(f));
                String factoryClassName = props.getProperty(FACTORY_ID);
                FactoryFinderCache.addCacheEntry(module.getClassLoader(), FACTORY_ID, factoryClassName);
            }
        } catch (Exception ex) {
        }


        // Use the system property
        try {
            String systemProp =
                    System.getProperty(FACTORY_ID);
            if (systemProp != null) {
                FactoryFinderCache.addCacheEntry(module.getClassLoader(), FACTORY_ID, systemProp);
            }
        } catch (SecurityException se) {
        }
        FactoryFinderCache.addCacheEntry(module.getClassLoader(), FACTORY_ID, ExpressionFactoryImpl.class.getName());
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if(module != null) {
            FactoryFinderCache.clearClassLoader(module.getClassLoader());
            try {
                purge.invoke(new BeanELResolver(), module.getClassLoader());
            } catch (Exception e) {
                WebLogger.ROOT_LOGGER.couldNotPurgeELCache(e);
            }
        }
    }
}
