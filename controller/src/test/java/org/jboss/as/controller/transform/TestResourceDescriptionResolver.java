package org.jboss.as.controller.transform;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class TestResourceDescriptionResolver implements ResourceDescriptionResolver {
    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return key;
            }

            @Override
            public Enumeration<String> getKeys() {
                return new Enumeration<String>() {
                    @Override
                    public boolean hasMoreElements() {
                        return false;
                    }

                    @Override
                    public String nextElement() {
                        return null;
                    }
                };
            }
        };
    }

    @Override
    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
        return "description";
    }

    @Override
    public String getResourceAttributeDescription(String attributeName, Locale locale, ResourceBundle bundle) {
        return attributeName;
    }

    @Override
    public String getResourceAttributeValueTypeDescription(String attributeName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return attributeName;
    }

    @Override
    public String getOperationDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return operationName;
    }

    @Override
    public String getOperationParameterDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle) {
        return operationName + "-" + paramName;
    }

    @Override
    public String getOperationParameterValueTypeDescription(String operationName, String paramName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return operationName + "-" + paramName;
    }

    @Override
    public String getOperationReplyDescription(String operationName, Locale locale, ResourceBundle bundle) {
        return operationName;
    }

    @Override
    public String getOperationReplyValueTypeDescription(String operationName, Locale locale, ResourceBundle bundle, String... suffixes) {
        return operationName;
    }

    @Override
    public String getChildTypeDescription(String childType, Locale locale, ResourceBundle bundle) {
        return childType;
    }
}
