package org.jboss.as.ee.component.deployers;

import java.lang.annotation.Annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;

/**
 * An annotation information factory that simply returns true if the annotation is present
 *
 * @author Stuart Douglas
 */
public class BooleanAnnotationInformationFactory<T extends Annotation> extends ClassAnnotationInformationFactory<T, Boolean> {

    public BooleanAnnotationInformationFactory(final Class<T> annotationType) {
        super(annotationType, null);
    }

    @Override
    protected Boolean fromAnnotation(final AnnotationInstance annotationInstance) {
        return true;
    }
}
