/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
