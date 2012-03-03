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

package org.jboss.as.capedwarf.api;

import org.jboss.as.capedwarf.services.ServletExecutor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Capedwarf listener.
 * * registers servlet context -- tasks API
 * * holds classloaders -- so we know which apps are CD apps from TCCL
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfListener implements ServletContextListener {

    private static Set<ClassLoader> classLoaders = new CopyOnWriteArraySet<ClassLoader>();

    static boolean isCapedwarfApp(ClassLoader classLoader) {
        return classLoaders.contains(classLoader);
    }
    
    static boolean isCapedwarfApp() {
        return isCapedwarfApp(SecurityActions.getAppClassLoader());
    }

    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        final String appId = (String) context.getAttribute("org.jboss.capedwarf.appId");
        ServletExecutor.registerContext(appId, context);
        classLoaders.add(SecurityActions.getAppClassLoader());
    }

    public void contextDestroyed(ServletContextEvent sce) {
        try {
            final ServletContext context = sce.getServletContext();
            final String appId = (String) context.getAttribute("org.jboss.capedwarf.appId");
            ServletExecutor.unregisterContext(appId);
        } finally {
            classLoaders.remove(SecurityActions.getAppClassLoader());
        }
    }
}
