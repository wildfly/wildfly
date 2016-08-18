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

package org.jboss.as.test.integration.deployment.structure.ear;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * User: jpai
 */
@Stateless
public class ClassLoadingEJB {

    @Resource(lookup = "java:module/ModuleName")
    private String moduleName;

    @Resource(lookup = "java:app/AppName")
    private String appName;

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (className == null || className.trim().isEmpty()) {
            throw new RuntimeException("Classname parameter cannot be null or empty");
        }
        return this.getClass().getClassLoader().loadClass(className);
    }

    public boolean hasResource(String resource) {
        return this.getClass().getResource(resource) != null;
    }

    public String query(String name) throws NamingException {
        return new InitialContext().lookup(name).toString();
    }

    public String getResourceModuleName() {
        return moduleName;
    }

    public String getResourceAppName() {
        return appName;
    }
}
