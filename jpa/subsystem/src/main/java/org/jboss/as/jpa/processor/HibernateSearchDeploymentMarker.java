package org.jboss.as.jpa.processor;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;

import java.util.List;

public class HibernateSearchDeploymentMarker {
    private static final AttachmentKey<AttachmentList<String>> KEY = AttachmentKey.createList(String.class);

    public static void markBackendType(DeploymentUnit unit, String backendType) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        unit.addToAttachmentList(KEY, backendType);
    }

    public static List<String> getBackendTypes(DeploymentUnit unit) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        return unit.getAttachmentList(KEY);
    }

    private HibernateSearchDeploymentMarker() {
    }

}
