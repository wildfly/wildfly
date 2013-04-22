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
package org.jboss.as.weld;

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.weld.Container;
import org.jboss.weld.Weld;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Service provider for {@link CDI}.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldProvider implements CDIProvider {

    private final Weld weld = new EnhancedWeld();

    @Override
    public CDI<Object> getCDI() {
        return weld;
    }

    private static class EnhancedWeld extends Weld {

        /**
         * <p>
         * If the called of a {@link CDI} method is placed outside of BDA we have two options:
         * </p>
         *
         * <ol>
         * <li>The class is placed in an archive with no beans.xml but with a CDI extension. In that case there exists an
         * additional BDA but {@link Weld#getBeanManager()} may not find it since the
         * {@link BeanDeploymentArchive#getBeanClasses()} of this logical BDA only contains extension classes or classes added
         * by an extension. Therefore, we are going to iterate over the additional BDAs and try to match them with the
         * classloader of the caller class.</li>
         *
         * <li>The class comes from an archive that is not a BDA nor bundles an extension. In that case we return the root BDA
         * which may be the BDA for WEB-INF/classes in war deployments or the root BDA of an ear.</li>
         * </ol>
         *
         */
        @Override
        protected BeanManagerImpl unsatisfiedBeanManager(String callerClassName) {
            BeanDeploymentArchiveImpl rootBda = findRootBda();
            if (rootBda == null) {
                return super.unsatisfiedBeanManager(callerClassName);
            }

            Class<?> callingClass = null;
            try {
                callingClass = rootBda.getModule().getClassLoader().loadClass(callerClassName);
            } catch (ClassNotFoundException e) {
                // fallback to the root bda manager
                return Container.instance().beanDeploymentArchives().get(rootBda);
            }

            BeanManagerImpl additionalBdaManager = findBeanManagerForAdditionalBdaWithMatchingClassloader(callingClass.getClassLoader());
            if (additionalBdaManager != null) {
                return additionalBdaManager;
            }

            // fallback to the root bda manager
            return Container.instance().beanDeploymentArchives().get(rootBda);
        }

        private BeanDeploymentArchiveImpl findRootBda() {
            for (Map.Entry<BeanDeploymentArchive, BeanManagerImpl> entry : Container.instance().beanDeploymentArchives().entrySet()) {
                if (entry.getKey() instanceof BeanDeploymentArchiveImpl) {
                    BeanDeploymentArchiveImpl bda = cast(entry.getKey());
                    if (bda.isRoot()) {
                        return bda;
                    }
                }
            }
            return null;
        }

        private BeanManagerImpl findBeanManagerForAdditionalBdaWithMatchingClassloader(ClassLoader classLoader) {
            for (Map.Entry<BeanDeploymentArchive, BeanManagerImpl> entry : Container.instance().beanDeploymentArchives().entrySet()) {
                if (entry.getKey() instanceof BeanDeploymentArchiveImpl) {
                    BeanDeploymentArchiveImpl bda = cast(entry.getKey());
                    if (bda.getId().endsWith(WeldDeployment.ADDITIONAL_CLASSES_BDA_SUFFIX)) {
                        if (bda.getModule() != null && bda.getModule().getClassLoader() != null && bda.getModule().getClassLoader().equals(classLoader)) {
                            return entry.getValue();
                        }
                    }
                }
            }
            return null;
        }
    }
}
