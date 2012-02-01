package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsSubsystemProviders {

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getSubsystemDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getSubsystemAddDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getSubsystemRemoveDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getSubsystemDescribeDescription(locale);
        }
    };

    static final DescriptionProvider STACK = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolStackDescription(locale);
        }
    };

    static final DescriptionProvider STACK_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolStackAddDescription(locale);
        }
    };

    static final DescriptionProvider STACK_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolStackRemoveDescription(locale);
        }
    };

    static final DescriptionProvider TRANSPORT = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getTransportDescription(locale);
        }
    };

    static final DescriptionProvider TRANSPORT_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getTransportAddDescription(locale);
        }
    };

    static final DescriptionProvider TRANSPORT_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getTransportRemoveDescription(locale);
        }
    };

    static final DescriptionProvider PROTOCOL = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolDescription(locale);
        }
    };

    static final DescriptionProvider PROTOCOL_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolAddDescription(locale);
        }
    };

    static final DescriptionProvider PROTOCOL_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolRemoveDescription(locale);
        }
    };


    static final DescriptionProvider PROTOCOL_PROPERTY = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolPropertyDescription(locale);
        }
    };
    static final DescriptionProvider PROTOCOL_PROPERTY_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolPropertyAddDescription(locale);
        }
    };
    static final DescriptionProvider PROTOCOL_PROPERTY_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolPropertyRemoveDescription(locale);
        }
    };
}
