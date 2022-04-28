package org.jboss.as.jsf.deployment;

import java.lang.annotation.Annotation;
import javax.faces.bean.ManagedBean;
import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import javax.faces.view.facelets.FaceletsResourceResolver;

import org.jboss.jandex.DotName;

enum FacesAnnotation {
    FACES_COMPONENT(FacesComponent.class),
    FACES_CONVERTER(FacesConverter.class),
    FACES_VALIDATOR(FacesValidator.class),
    FACES_RENDERER(FacesRenderer.class),
    MANAGED_BEAN(ManagedBean.class),
    NAMED_EVENT(NamedEvent.class),
    FACES_BEHAVIOR(FacesBehavior.class),
    FACES_BEHAVIOR_RENDERER(FacesBehaviorRenderer.class),
    FACELETS_RESOURCE_RESOLVER(FaceletsResourceResolver.class);

    final Class<? extends Annotation> annotationClass;
    final DotName indexName;

    private FacesAnnotation(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
        this.indexName = DotName.createSimple(annotationClass.getName());
    }
}
