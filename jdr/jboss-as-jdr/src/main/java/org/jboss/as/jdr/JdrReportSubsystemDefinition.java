package org.jboss.as.jdr;

import org.jboss.as.controller.SimpleResourceDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JdrReportSubsystemDefinition extends SimpleResourceDefinition {
    static final JdrReportSubsystemDefinition INSTANCE = new JdrReportSubsystemDefinition();

    private JdrReportSubsystemDefinition() {
        super(JdrReportExtension.SUBSYSTEM_PATH, JdrReportExtension.getResourceDescriptionResolver(),
                JdrReportSubsystemAdd.INSTANCE,
                JdrReportSubsystemRemove.INSTANCE);
    }

}
