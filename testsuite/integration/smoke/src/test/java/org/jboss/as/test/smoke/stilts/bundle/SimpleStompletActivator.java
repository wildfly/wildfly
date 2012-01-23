/*
 * Copyright 2011 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.smoke.stilts.bundle;


import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.repository.RepositoryConstants;
import org.jboss.osgi.repository.RepositoryRequirementBuilder;
import org.jboss.osgi.repository.XRepository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.Repository;
import org.projectodd.stilts.stomplet.Stomplet;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.jboss.as.test.smoke.stilts.bundle.SimpleStomplet.DESTINATION_QUEUE_ONE;


/**
 * The BundleActivator for a simple {@link Stomplet) deployment.
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Sep-2011
 */
public class SimpleStompletActivator implements BundleActivator {

    static Logger log = Logger.getLogger(SimpleStompletActivator.class);

    private ServiceRegistration registration;

    @Override
    public void start(BundleContext context) throws Exception {
        log.infof("start: %s", context);

        provideStiltsServer(context);

        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("destinationPattern", DESTINATION_QUEUE_ONE);
        registration = context.registerService(Stomplet.class.getName(), new SimpleStomplet(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.infof("stop: %s", context);
        if (registration != null)
            registration.unregister();
    }

    private void provideStiltsServer(BundleContext context) throws BundleException {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin padmin = (PackageAdmin) context.getService(sref);
        if (padmin.getBundles("stilts-stomplet-server-bundle", null) == null) {
            installSupportBundle(context, ModuleIdentifier.create("org.jboss.netty"));
            installSupportBundle(context, ModuleIdentifier.create("org.projectodd.stilts")).start();
        }
    }

    private Bundle installSupportBundle(BundleContext context, ModuleIdentifier moduleid) throws BundleException {
        XRepository repository = (XRepository) getRepository(context);
        RepositoryRequirementBuilder builder = repository.getRequirementBuilder();
        Requirement req = builder.createArtifactRequirement(moduleid);
        Capability cap = repository.findProviders(req).iterator().next();
        URL location = (URL) cap.getAttributes().get(RepositoryConstants.CONTENT_URL);
        return context.installBundle(location.toExternalForm());
    }

    private Repository getRepository(BundleContext context) {
        ServiceReference sref = context.getServiceReference(Repository.class.getName());
        return (Repository) context.getService(sref);
    }
}
