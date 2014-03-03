/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security.service;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.SecurityLogger;
import org.jboss.as.security.SecurityMessages;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.SubjectFactory;
import org.jboss.security.plugins.JBossSecuritySubjectFactory;

/**
 * SubjectFactory service for the security container
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SubjectFactoryService implements Service<SubjectFactory> {

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("subject-factory");

    private static final SecurityLogger log = SecurityLogger.ROOT_LOGGER;

    private final InjectedValue<ISecurityManagement> securityManagementValue = new InjectedValue<ISecurityManagement>();

    private SubjectFactory subjectFactory;

    private String subjectFactoryClassName;

    public SubjectFactoryService(String subjectFactoryClassName) {
        this.subjectFactoryClassName = subjectFactoryClassName;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        log.debugf("Starting SubjectFactoryService");
        final ISecurityManagement injectedSecurityManagement = securityManagementValue.getValue();
        int i = subjectFactoryClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("subject-factory-class-name attribute");
        String moduleSpec = subjectFactoryClassName.substring(0, i);
        String className = subjectFactoryClassName.substring(i + 1);
        JBossSecuritySubjectFactory subjectFactory = null;
        try {
            Class<?> subjectFactoryClazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
            subjectFactory = (JBossSecuritySubjectFactory) subjectFactoryClazz.newInstance();
        } catch (Exception e) {
            throw SecurityMessages.MESSAGES.unableToStartException("SubjectFactoryService", e);
        }
        subjectFactory.setSecurityManagement(injectedSecurityManagement);
        this.subjectFactory = subjectFactory;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public synchronized SubjectFactory getValue() throws IllegalStateException {
        return subjectFactory;
    }

    /**
     * Target {@code Injector}
     *
     * @return target
     */
    public Injector<ISecurityManagement> getSecurityManagementInjector() {
        return securityManagementValue;
    }

}
