/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentPhaseContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.metadata.annotation.creator.DeclareRolesProcessor;
import org.jboss.metadata.annotation.creator.web.MultipartConfigProcessor;
import org.jboss.metadata.annotation.creator.web.RunAsProcessor;
import org.jboss.metadata.annotation.creator.web.ServletSecurityProcessor;
import org.jboss.metadata.annotation.creator.web.WebFilterProcessor;
import org.jboss.metadata.annotation.creator.web.WebListenerProcessor;
import org.jboss.metadata.annotation.creator.web.WebServletProcessor;
import org.jboss.metadata.annotation.finder.AnnotationFinder;
import org.jboss.metadata.annotation.finder.DefaultAnnotationFinder;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.metadata.web.spec.AnnotationsMetaData;
import org.jboss.metadata.web.spec.Web30MetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.modules.Module;

/**
 * Web annotation deployment processor.
 *
 * @author Emanuel Muckenhuber
 * @author Remy Maucherat
 */
public class WarAnnotationDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName webFilter = DotName.createSimple(WebFilter.class.getName());
    private static final DotName webListener = DotName.createSimple(WebListener.class.getName());
    private static final DotName webServlet = DotName.createSimple(WebServlet.class.getName());
    private static final DotName runAs = DotName.createSimple(RunAs.class.getName());
    private static final DotName declareRoles = DotName.createSimple(DeclareRoles.class.getName());
    private static final DotName multipartConfig = DotName.createSimple(MultipartConfig.class.getName());
    private static final DotName servletSecurity = DotName.createSimple(ServletSecurity.class.getName());

    /**
     * Process web annotations.
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if(!isWarDeployment(phaseContext)) {
            return; // Skip non web deployments
        }
        final WarAnnotationIndex index = phaseContext.getAttachment(WarAnnotationIndexProcessor.ATTACHMENT_KEY);
        if (index == null) {
            return; // Skip if there is no annotation index
        }
        WarMetaData warMetaData = phaseContext.getAttachment(WarMetaData.ATTACHMENT_KEY);
        assert warMetaData != null;
        Map<String, WebMetaData> annotationsMetaData = warMetaData.getAnnotationsMetaData();
        if (annotationsMetaData == null) {
            annotationsMetaData = new HashMap<String, WebMetaData>();
            warMetaData.setAnnotationsMetaData(annotationsMetaData);
        }
        final Module module = phaseContext.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null) {
            throw new DeploymentUnitProcessingException("failed to resolve module for deployment " + phaseContext.getName());
        }
        final ClassLoader classLoader = module.getClassLoader();

        // Process web-inf/classes
        if (index.getRootIndex() != null) {
            annotationsMetaData.put("", processAnnotations(index.getRootIndex(), classLoader));
        }

        // Process lib/*.jar
        for(final String pathName : index.getPathNames()) {
            final Index jarIndex = index.getIndex(pathName);
            annotationsMetaData.put(pathName, processAnnotations(jarIndex, classLoader));
        }
    }

    /**
     * Process a single index.
     *
     * @param index the annotation index
     * @param classLoader the module classloader
     * @throws DeploymentUnitProcessingException
     */
    protected WebMetaData processAnnotations(Index index, ClassLoader classLoader)
    throws DeploymentUnitProcessingException {
        Web30MetaData metaData = new Web30MetaData();
        AnnotationFinder<AnnotatedElement> finder = new DefaultAnnotationFinder<AnnotatedElement>();
        // @WebServlet
        final List<AnnotationTarget> webServletAnnotationTargets = index.getAnnotationTargets(webServlet);
        if (webServletAnnotationTargets != null && webServletAnnotationTargets.size() > 0) {
            WebServletProcessor processor = new WebServletProcessor(finder);
            for (final AnnotationTarget target : webServletAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@WebServlet is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @WebServlet on " + target);
                }
                if (type != null) {
                    processor.process(metaData, type);
                }
            }
        }
        // @WebFilter
        final List<AnnotationTarget> webFilterAnnotationTargets = index.getAnnotationTargets(webFilter);
        if (webFilterAnnotationTargets != null && webFilterAnnotationTargets.size() > 0) {
            WebFilterProcessor processor = new WebFilterProcessor(finder);
            for (final AnnotationTarget target : webFilterAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@WebFilter is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @WebFilter on " + target);
                }
                if (type != null) {
                    processor.process(metaData, type);
                }
            }
        }
        // @WebListener
        final List<AnnotationTarget> webListenerAnnotationTargets = index.getAnnotationTargets(webListener);
        if (webListenerAnnotationTargets != null && webListenerAnnotationTargets.size() > 0) {
            WebListenerProcessor processor = new WebListenerProcessor(finder);
            for (final AnnotationTarget target : webListenerAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@WebListener is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @WebListener on " + target);
                }
                if (type != null) {
                    processor.process(metaData, type);
                }
            }
        }
        // @RunAs
        final List<AnnotationTarget> runAsAnnotationTargets = index.getAnnotationTargets(runAs);
        if (runAsAnnotationTargets != null && runAsAnnotationTargets.size() > 0) {
            RunAsProcessor processor = new RunAsProcessor(finder);
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
               annotations = new AnnotationsMetaData();
               metaData.setAnnotations(annotations);
            }
            for (final AnnotationTarget target : runAsAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@RunAs is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @RunAs on " + target);
                }
                if (type != null) {
                    processor.process(annotations, type);
                }
            }
        }
        // @DeclareRoles
        final List<AnnotationTarget> declareRolesAnnotationTargets = index.getAnnotationTargets(declareRoles);
        if (declareRolesAnnotationTargets != null && declareRolesAnnotationTargets.size() > 0) {
            DeclareRolesProcessor processor = new DeclareRolesProcessor(finder);
            SecurityRolesMetaData securityRoles = metaData.getSecurityRoles();
            if (securityRoles == null) {
               securityRoles = new SecurityRolesMetaData();
               metaData.setSecurityRoles(securityRoles);
            }
            for (final AnnotationTarget target : declareRolesAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@DeclareRoles is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @DeclareRoles on " + target);
                }
                if (type != null) {
                    processor.process(securityRoles, type);
                }
            }
        }
        // @MultipartConfig
        final List<AnnotationTarget> multipartConfigAnnotationTargets = index.getAnnotationTargets(multipartConfig);
        if (multipartConfigAnnotationTargets != null && multipartConfigAnnotationTargets.size() > 0) {
            MultipartConfigProcessor processor = new MultipartConfigProcessor(finder);
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
               annotations = new AnnotationsMetaData();
               metaData.setAnnotations(annotations);
            }
            for (final AnnotationTarget target : multipartConfigAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@MultipartConfig is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @MultipartConfig on " + target);
                }
                if (type != null) {
                    processor.process(annotations, type);
                }
            }
        }
        // @ServletSecurity
        final List<AnnotationTarget> servletSecurityAnnotationTargets = index.getAnnotationTargets(servletSecurity);
        if (servletSecurityAnnotationTargets != null && servletSecurityAnnotationTargets.size() > 0) {
            ServletSecurityProcessor processor = new ServletSecurityProcessor(finder);
            AnnotationsMetaData annotations = metaData.getAnnotations();
            if (annotations == null) {
               annotations = new AnnotationsMetaData();
               metaData.setAnnotations(annotations);
            }
            for (final AnnotationTarget target : servletSecurityAnnotationTargets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@ServletSecurity is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                Class<?> type = null;
                try {
                    type = classLoader.loadClass(classInfo.name().toString());
                } catch (Exception e) {
                    throw new DeploymentUnitProcessingException("Could not process @ServletSecurity on " + target);
                }
                if (type != null) {
                    processor.process(annotations, type);
                }
            }
        }
        return metaData;
    }

}
