package org.jboss.as.server.deploymentoverlay.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
public class DeploymentOverlayService implements Service<DeploymentOverlayService> {

    public static final ServiceName SERVICE_NAME = DeploymentOverlayIndexService.SERVICE_NAME.append("deploymentOverlayService");

    private final Set<ContentService> contentServices = new CopyOnWriteArraySet<ContentService>();
    private final String name;

    public DeploymentOverlayService(final String name) {
        this.name = name;
    }

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
    }

    public void addContentService(ContentService service) {
        contentServices.add(service);
    }

    public void removeContentService(ContentService service) {
        contentServices.remove(service);
    }

    @Override
    public DeploymentOverlayService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Set<ContentService> getContentServices() {
        return contentServices;
    }

    public String getName() {
        return name;
    }
}
