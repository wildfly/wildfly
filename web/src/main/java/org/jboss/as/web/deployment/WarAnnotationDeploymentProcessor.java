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

import java.util.List;

import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

/**
 * Web annotation deployment processor.
 *
 * @author Emanuel Muckenhuber
 */
public class WarAnnotationDeploymentProcessor implements DeploymentUnitProcessor {

    public static final long PRIORITY = DeploymentPhases.POST_MODULE_DESCRIPTORS.plus(300L);

    private static final DotName webFilter = DotName.createSimple(WebFilter.class.getName());
    private static final DotName webListener = DotName.createSimple(WebListener.class.getName());
    private static final DotName webServlet = DotName.createSimple(WebServlet.class.getName());

    /**
     * Process web annotations.
     *
     * TODO this should be based on the xml metadata
     *
     * {@inheritDoc}
     */
    public void processDeployment(final DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final WarAnnotationIndex index = context.getAttachment(WarAnnotationIndexProcessor.ATTACHMENT_KEY);
        if (index == null) {
            return; // Skip if there is no annotation index
        }
        // process web-inf/classes
        processAnnotations(index.getRootIndex());

        // Process lib/*.jar
        for(final String pathName : index.getPathNames()) {
            final Index jarIndex = index.getIndex(pathName);
            processAnnotations(jarIndex);
        }
        // TODO process annotations on both - the resources found here, plus the
        // ones from the parsed metadata
    }

    /**
     * Process a single index.
     *
     * TODO this should be again based on the web.xml / fragment metadata
     *
     * @param index the annotation index
     * @throws DeploymentUnitProcessingException
     */
    void processAnnotations(Index index) throws DeploymentUnitProcessingException {
        if(index == null) {
            return;
        }
        // web servlets
        final List<AnnotationTarget> servlets = index.getAnnotationTargets(webServlet);
        if (servlets != null && servlets.size() > 0) {
            for (final AnnotationTarget target : servlets) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@WebServlet is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                classInfo.toString(); // servlet
            }
        }
        // web filters
        final List<AnnotationTarget> filters = index.getAnnotationTargets(webFilter);
        if (filters != null && filters.size() > 0) {
            for (final AnnotationTarget target : filters) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@WebFilter is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                classInfo.toString(); // filter
            }
        }
        // web listeners
        final List<AnnotationTarget> listeners = index.getAnnotationTargets(webListener);
        if (listeners != null && listeners.size() > 0) {
            for (final AnnotationTarget target : filters) {
                if (!(target instanceof ClassInfo)) {
                    throw new DeploymentUnitProcessingException("@WebListener is only allowed at class level " + target);
                }
                ClassInfo classInfo = ClassInfo.class.cast(target);
                classInfo.toString(); // listener
            }
        }
    }

}
