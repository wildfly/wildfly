package org.jboss.as.server.deployment.scanner;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stuart Douglas
 */
public interface DeploymentOperations extends Closeable {

    Future<ModelNode> deploy(final ModelNode operation, final ScheduledExecutorService scheduledExecutor);

    Set<String> getDeploymentNames();

}
