package org.jboss.as.web;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * Common web description providers.
 *
 * @author Emanuel Muckenhuber
 */
class WebSubsystemDescriptionProviders {

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

    public static final DescriptionProvider DEPLOYMENT = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getDeploymentRuntimeDescription(locale);
        }
    };

    public static final DescriptionProvider SERVLET = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getDeploymentServletDescription(locale);
        }
    };

    public static DescriptionProvider SSL = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getSSLDescription(locale);
        }
    };

}
