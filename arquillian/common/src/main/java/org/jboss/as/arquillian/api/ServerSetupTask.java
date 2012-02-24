package org.jboss.as.arquillian.api;

import org.jboss.as.arquillian.container.ManagementClient;

/**
 *
 * A task which is run before deployment that allows the client to customize the server config.
 *
 * @author Stuart Douglas
 */
public interface ServerSetupTask {

    void setup(final ManagementClient managementClient);

    void tearDown(final ManagementClient managementClient);
}
