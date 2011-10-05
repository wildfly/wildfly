package org.jboss.as.modcluster;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * Mod_cluster description providers.
 *
 * @author Emanuel Muckenhuber
 */
public class ModClusterSubsystemDescriptionProviders {

    public static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return ModClusterSubsystemDescriptions.getSubsystemDescription(locale);
        }
    };
    public static DescriptionProvider SSL = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return ModClusterSubsystemDescriptions.getSSLDescription(locale);
        }
    };
}
