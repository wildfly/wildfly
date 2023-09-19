package org.wildfly.ee.feature.pack.layer.tests.jsf;

import jakarta.faces.FacesWrapper;

public class JsfClassFromRootPackageUsage implements FacesWrapper<String> {
    @Override
    public String getWrapped() {
        return null;
    }
}
