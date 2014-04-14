/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.audit;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.as.controller.audit.spi.CustomAuditLogEventFormatterFactory;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
class SecurityActions {

    static CustomAuditLogEventFormatterFactory createAuditLogEventFormatterFactory(final String moduleSpec, final String code) throws Exception {

        if (WildFlySecurityManager.isChecking()){
            try {
                return WildFlySecurityManager.doChecked(new PrivilegedExceptionAction<CustomAuditLogEventFormatterFactory>() {
                    public CustomAuditLogEventFormatterFactory run() throws Exception {
                        return internalCreateAuditLogEventFormatterFactory(moduleSpec, code);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof Exception){
                    throw (Exception)t;
                }
                throw new RuntimeException(t);
            }
        }
        return internalCreateAuditLogEventFormatterFactory(moduleSpec, code);
    }

    private static CustomAuditLogEventFormatterFactory internalCreateAuditLogEventFormatterFactory(String moduleSpec, String code) throws Exception {
        ModuleLoader loader = Module.getCallerModuleLoader();
        final Module module = loader.loadModule(ModuleIdentifier.fromString(moduleSpec));
        Class<?> clazz = module.getClassLoader().loadClass(code);
        return CustomAuditLogEventFormatterFactory.class.cast(clazz.newInstance());
    }
}
