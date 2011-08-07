package org.jboss.as.xts;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

public class Descriptions {

    static final String RESOURCE_NAME = Descriptions.class.getPackage().getName() + ".LocalDescriptions";

    static ModelNode getSubsystem(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();

        subsystem.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("xts"));
        subsystem.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(ModelDescriptionConstants.NAMESPACE).set(org.jboss.as.xts.Namespace.XTS_1_0.getUriString());
        // xts-environment
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("xts-environment"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(false);
        // xts-environment.url
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.URL, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("xts-environment.url"));
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.URL, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        subsystem.get(ModelDescriptionConstants.ATTRIBUTES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.URL, ModelDescriptionConstants.REQUIRED).set(false);

        return subsystem;
    }

    static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();

        op.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("xts.add"));
        op.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        op.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        op.get(ModelDescriptionConstants.NAMESPACE).set(org.jboss.as.xts.Namespace.XTS_1_0.getUriString());
        // xts-environment
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("xts-environment"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.REQUIRED).set(false);
        // xts-environment.url
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.URL, ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("xts-environment.url"));
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.URL, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        op.get(ModelDescriptionConstants.REQUEST_PROPERTIES, CommonAttributes.XTS_ENVIRONMENT, ModelDescriptionConstants.VALUE_TYPE, ModelDescriptionConstants.URL, ModelDescriptionConstants.REQUIRED).set(false);

        op.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
