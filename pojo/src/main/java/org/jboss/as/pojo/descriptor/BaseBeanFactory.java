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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.pojo.api.BeanFactory;
import org.jboss.as.pojo.service.BeanInfo;
import org.jboss.as.pojo.service.BeanUtils;
import org.jboss.as.pojo.service.DefaultBeanInfo;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Base bean factory.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BaseBeanFactory implements BeanFactory {

    private BeanMetaDataConfig bmd;

    @SuppressWarnings("unchecked")
    public Object create() throws Throwable {
        Module module = bmd.getModule().getInjectedModule().getValue();
        final SecurityManager sm = System.getSecurityManager();
        ClassLoader moduleClassLoader;
        if (sm == null) {
            moduleClassLoader = module.getClassLoader();
        } else {
            moduleClassLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> module.getClassLoader());
        }
        Class<?> beanClass = moduleClassLoader.loadClass(bmd.getBeanClass());
        DeploymentReflectionIndex index;
        if (sm == null) {
            index = DeploymentReflectionIndex.create();
        } else {
            index = AccessController.doPrivileged((PrivilegedAction<DeploymentReflectionIndex>) () -> DeploymentReflectionIndex.create());
        }
        BeanInfo beanInfo = new DefaultBeanInfo(index, beanClass);
        Object result = BeanUtils.instantiateBean(bmd, beanInfo, index, module);
        BeanUtils.configure(bmd, beanInfo, module, result, false);
        BeanUtils.dispatchLifecycleJoinpoint(beanInfo, result, bmd.getCreate(), "create");
        BeanUtils.dispatchLifecycleJoinpoint(beanInfo, result, bmd.getStart(), "start");
        return result;
    }

    public void setBmd(BeanMetaDataConfig bmd) {
        this.bmd = bmd;
    }
}
