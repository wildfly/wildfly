package org.jboss.as.jpa.processor;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;

import java.util.List;

public class HibernateSearchDeploymentMarker {
    private static final AttachmentKey<AttachmentList<String>> BACKEND_TYPE_KEY = AttachmentKey.createList(String.class);
    private static final AttachmentKey<AttachmentList<String>> COORDINATION_STRATEGY_KEY = AttachmentKey.createList(String.class);

    public static void markBackendType(DeploymentUnit unit, String backendType) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        unit.addToAttachmentList(BACKEND_TYPE_KEY, backendType);
    }

    public static List<String> getBackendTypes(DeploymentUnit unit) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        return unit.getAttachmentList(BACKEND_TYPE_KEY);
    }

    public static void markCoordinationStrategy(DeploymentUnit unit, String coordinationStrategy) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        unit.addToAttachmentList(COORDINATION_STRATEGY_KEY, coordinationStrategy);
    }

    public static List<String> getCoordinationStrategies(DeploymentUnit unit) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        return unit.getAttachmentList(COORDINATION_STRATEGY_KEY);
    }

    private HibernateSearchDeploymentMarker() {
    }

}
