/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.jsf.injection;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.faces.bean.ManagedBean;
import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.context.ExternalContext;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import javax.servlet.ServletContext;

/**
 * This class retrieves the annotation map from application scope.  This map was placed there by the JSFAnnotationProcessor
 * in the jsf subsystem.
 *
 * The class also reloads the map if needed.  The reason why the map must be reloaded is because the JSF Annotation classes used as the map keys are always
 * loaded by the JSF subsystem and thus always correspond to the default JSF implementation.  If a different JSF
 * implementation is used then the JSF impl will be looking for the wrong version of the map keys.  So, we replace
 * the default implementations of the JSF Annotation classes with whatever version the WAR is actually using.
 *
 * The reason this works is because we have a "slot" for jsf-injection for each JSF implementation.  And jsf-injection
 * points to its corresponding JSF impl/api slots.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class AnnotationMap {
    /**
     * @see org.jboss.as.jsf.deployment.JSFAnnotationProcessor#FACES_ANNOTATIONS_SC_ATTR
     */
    public static final String FACES_ANNOTATIONS_SC_ATTR =  "org.jboss.as.jsf.FACES_ANNOTATIONS";
    private static final String ANNOTATION_MAP_CONVERTED = "org.jboss.as.jsf.ANNOTATION_MAP_CONVERTED";

    private static final Map<String, Class<? extends Annotation>> stringToAnnoMap = new HashMap<String, Class<? extends Annotation>>();

    static {
        // These classes need to be loaded in order!  Some can't be loaded if the JSF version is too old.

        try { // all of the following classes are available from JSF 2.0 and JSF 2.1
            stringToAnnoMap.put(FacesComponent.class.getName(), FacesComponent.class);
            stringToAnnoMap.put(FacesConverter.class.getName(), FacesConverter.class);
            stringToAnnoMap.put(FacesValidator.class.getName(), FacesValidator.class);
            stringToAnnoMap.put(FacesRenderer.class.getName(), FacesRenderer.class);
            stringToAnnoMap.put(ManagedBean.class.getName(), ManagedBean.class);
            stringToAnnoMap.put(NamedEvent.class.getName(), NamedEvent.class);
            stringToAnnoMap.put(FacesBehavior.class.getName(), FacesBehavior.class);
            stringToAnnoMap.put(FacesBehaviorRenderer.class.getName(), FacesBehaviorRenderer.class);

            // Put JSF 2.2 annotations below this line if any new ones are to be scanned.  So far none.
        } catch (Exception e) {
            // Ignore.  Whatever classes are available have been loaded into the map.
        }
    }

    // don't allow instance
    private AnnotationMap() {}

    public static Map<Class<? extends Annotation>, Set<Class<?>>> get(final ExternalContext extContext) {
        Map<String, Object> appMap = extContext.getApplicationMap();
        if (appMap.get(ANNOTATION_MAP_CONVERTED) != null) {
            return (Map<Class<? extends Annotation>, Set<Class<?>>>)appMap.get(FACES_ANNOTATIONS_SC_ATTR);
        } else {
            appMap.put(ANNOTATION_MAP_CONVERTED, Boolean.TRUE);
            return convert((Map<Class<? extends Annotation>, Set<Class<?>>>)appMap.get(FACES_ANNOTATIONS_SC_ATTR));
        }
    }

    public static Map<Class<? extends Annotation>, Set<Class<?>>> get(final ServletContext servletContext) {
        Map<Class<? extends Annotation>, Set<Class<?>>> annotations = (Map<Class<? extends Annotation>, Set<Class<?>>>) servletContext.getAttribute(FACES_ANNOTATIONS_SC_ATTR);
        if (servletContext.getAttribute(ANNOTATION_MAP_CONVERTED) != null) {
            return annotations;
        } else {
            servletContext.setAttribute(ANNOTATION_MAP_CONVERTED, Boolean.TRUE);
            return convert(annotations);
        }
    }

    private static Map<Class<? extends Annotation>, Set<Class<?>>> convert(Map<Class<? extends Annotation>, Set<Class<?>>> annotations) {
        final Map<Class<? extends Annotation>, Set<Class<?>>> convertedAnnotatedClasses = new HashMap<Class<? extends Annotation>, Set<Class<?>>>();
        for (Map.Entry<Class<? extends Annotation>, Set<Class<?>>> entry : annotations.entrySet()) {
            final Class<? extends Annotation> annotation = entry.getKey();
            final Set<Class<?>> annotated = entry.getValue();
            final Class<? extends Annotation> knownAnnotation = stringToAnnoMap.get(annotation.getName());
            if (knownAnnotation != null) {
                convertedAnnotatedClasses.put(knownAnnotation, annotated); // put back in the map with the proper version of the class
            } else {
                // just copy over the original annotation to annotated classes mapping
                convertedAnnotatedClasses.put(annotation, annotated);
            }
        }

        return convertedAnnotatedClasses;
    }
}
