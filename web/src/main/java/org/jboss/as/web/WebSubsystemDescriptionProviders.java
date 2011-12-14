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

    public static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getSubsystemRemoveDescription(locale);
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

    public static DescriptionProvider ACCESS_LOG = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getAccessLogDescription(locale);
        }

    };

    public static DescriptionProvider DIRECTORY = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getDirectoryDescription(locale);
        }

    };

    public static DescriptionProvider SSO  = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getSSODescription(locale);
        }

    };

    public static DescriptionProvider REWRITE  = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getReWriteDescription(locale);
        }

    };

    public static DescriptionProvider REWRITECOND = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getReWriteCondDescription(locale);
        }

    };

    public static final DescriptionProvider JSP_CONFIGURATION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getJspConfigurationDescription(locale);
        }

    };

    public static final DescriptionProvider STATIC_RESOURCES = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getStaticResourceDescription(locale);
        }

    };

    public static DescriptionProvider CONFIGURATION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getConfigurationDescription(locale);
        }

    };

    public static DescriptionProvider CONTAINER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return WebSubsystemDescriptions.getContainerDescription(locale);
        }

    };

}
