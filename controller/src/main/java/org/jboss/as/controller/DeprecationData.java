package org.jboss.as.controller;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public final class DeprecationData {
    private ModelVersion since;

    public DeprecationData(ModelVersion since) {
        this.since = since;
    }

    public ModelVersion getSince() {
        return since;
    }
}
