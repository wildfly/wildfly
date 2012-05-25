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


import java.io.InputStream;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.resolver.XRequirement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.repository.RepositoryContent;


public class StompletServerActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin padmin = (PackageAdmin) context.getService(sref);
        if (padmin.getBundles("stilts-stomplet-server-bundle", null) == null) {
            installSupportBundle(context, ModuleIdentifier.create("org.jboss.netty"));
            installSupportBundle(context, ModuleIdentifier.create("org.projectodd.stilts")).start();
        }
    }

    private Bundle installSupportBundle(BundleContext context, ModuleIdentifier moduleid) throws BundleException {
        XRepository repository = getRepository(context);
        XRequirement req = XRequirementBuilder.create(moduleid).getRequirement();
        Capability cap = repository.findProviders(req).iterator().next();
        InputStream input = ((RepositoryContent)cap.getResource()).getContent();
        return context.installBundle(moduleid.toString(), input);
    }

    private XRepository getRepository(BundleContext context) {
        ServiceReference sref = context.getServiceReference(XRepository.class.getName());
        return (XRepository) context.getService(sref);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
