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

package org.jboss.as.arquillian.service;

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.junit.runner.RunWith;

/**
 * Service responsible for creating and managing the life-cycle of the Arquillian service.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ArquillianJunitAnnotationProcessor implements DeploymentUnitProcessor {

    private static final DotName JUNIT_ANNOTATION_NAME = DotName.createSimple(RunWith.class.getName());

    @Override
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {

        ArquillianConfig config = context.getAttachment(ArquillianConfig.ATTACHMENT_KEY);
        if (config == null) {
            return; // Skip if the configurations already exist
        }

        final Index index = context.getAttachment(AnnotationIndexProcessor.ATTACHMENT_KEY);
        if (index == null) {
            return; // Skip if there is no annotation index
        }

        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null) {
            throw new DeploymentUnitProcessingException("Arquillian annotation processing requires a module.");
        }

        List<Class<?>> testClasses = new ArrayList<Class<?>>();
        addJunitTestClasses(testClasses, module, index, JUNIT_ANNOTATION_NAME);

        if (testClasses.size() > 0) {
            config.setTestClasses(testClasses);
            context.putAttachment(ArquillianConfig.ATTACHMENT_KEY, config);
        }
    }

    private void addJunitTestClasses(List<Class<?>> testClasses, Module module, Index index, DotName annotationName) throws DeploymentUnitProcessingException {
        final List<AnnotationTarget> targets = index.getAnnotationTargets(annotationName);
        if (targets == null) {
            return; // Skip if there are no @RunWith annotations
        }

        final ClassLoader classLoader = module.getClassLoader();

        for (AnnotationTarget target : targets) {
            if (target instanceof ClassInfo == false) {
                continue;
            }
            final ClassInfo classInfo = (ClassInfo)target;
            final String testClassName = classInfo.name().toString();
            final Class<?> testClass;
            try {
                testClass = classLoader.loadClass(testClassName);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Could not load test class " + testClassName);
            }

            RunWith runWith = testClass.getAnnotation(RunWith.class);
            if (runWith.value().equals(Arquillian.class)) {
                testClasses.add(testClass);
            }
        }
    }

}
