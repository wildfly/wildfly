package org.jboss.as.jsf.deployment;

import java.lang.annotation.Annotation;
import jakarta.faces.bean.ManagedBean;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.event.NamedEvent;
import jakarta.faces.render.FacesBehaviorRenderer;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.view.facelets.FaceletsResourceResolver;

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
