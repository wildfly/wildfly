package org.jboss.as.webservices.deployers;

import org.jboss.ws.api.annotation.WebContext;

import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.metadata.property.PropertyReplacer;

/**
 * User: rsearls
 * Date: 7/17/14
 */
public class WebContextAnnotationInformationFactory extends
    ClassAnnotationInformationFactory<WebContext, WebContextAnnotationInfo> {

    protected WebContextAnnotationInformationFactory() {
        super(org.jboss.ws.api.annotation.WebContext.class, null);
    }

    @Override
    protected WebContextAnnotationInfo fromAnnotation(final AnnotationInstance annotationInstance, final PropertyReplacer propertyReplacer) {
        String authMethodValue = asString(annotationInstance, "authMethod");
        String contextRootValue = asString(annotationInstance, "contextRoot");
        boolean secureWSDLAccessValue = asBoolean(annotationInstance, "secureWSDLAccessValue");
        String transportGuaranteeValue = asString(annotationInstance, "transportGuarantee");
        String urlPatternValue = asString(annotationInstance, "urlPattern");
        String virtualHostValue = asString(annotationInstance, "virtualHost");
        return new  WebContextAnnotationInfo(authMethodValue, contextRootValue,
            secureWSDLAccessValue, transportGuaranteeValue, urlPatternValue, virtualHostValue);
    }


    private String asString(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? "" : value.asString();
    }

    private boolean asBoolean(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? false : Boolean.getBoolean(value.asString());
    }
}
