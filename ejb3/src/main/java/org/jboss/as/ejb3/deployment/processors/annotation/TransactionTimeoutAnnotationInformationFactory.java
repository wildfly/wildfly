package org.jboss.as.ejb3.deployment.processors.annotation;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.as.ejb3.tx.TransactionTimeoutDetails;

import java.util.concurrent.TimeUnit;

public class TransactionTimeoutAnnotationInformationFactory extends ClassAnnotationInformationFactory<TransactionTimeout, TransactionTimeoutDetails> {

    protected TransactionTimeoutAnnotationInformationFactory() {
        super(TransactionTimeout.class, null);
    }

    @Override
    protected TransactionTimeoutDetails fromAnnotation(final AnnotationInstance annotationInstance) {
        final long timeout = annotationInstance.value().asLong();
        AnnotationValue unitAnnVal = annotationInstance.value("unit");
        final TimeUnit unit = unitAnnVal != null ? TimeUnit.valueOf(unitAnnVal.asEnum()) : TimeUnit.SECONDS;
        return new TransactionTimeoutDetails(timeout, unit);
    }
}