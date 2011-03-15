package org.jboss.as.web;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

/**
 * Common web description providers.
 *
 * @author Emanuel Muckenhuber
 */
public class WebSubsystemDescriptionProviders {

    public static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getSubsystemDescription(locale);
        }
    };

    public static final DescriptionProvider CONNECTOR = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getConnectorDescription(locale);
        }
    };

    public static final DescriptionProvider VIRTUAL_SERVER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getVirtualServerDescription(locale);
        }
    };

}
