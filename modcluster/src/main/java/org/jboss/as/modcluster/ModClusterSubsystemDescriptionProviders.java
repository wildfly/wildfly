package org.jboss.as.modcluster;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

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
}
