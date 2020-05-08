/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.domain.mixed.eap730;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createEmptyOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.mixed.DomainAdjuster;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 7.3.0 legacy slaves.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainAdjuster730 extends DomainAdjuster {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withMasterServers) throws Exception {
        final List<ModelNode> ops = new ArrayList<>();

        removeMicroProfileJWT(ops, profileAddress.append(SUBSYSTEM, "microprofile-jwt-smallrye"));
        adjustUndertow(ops, profileAddress.append(SUBSYSTEM, "undertow"));
        adjustOpenTracing(ops, profileAddress.append(SUBSYSTEM, "microprofile-opentracing-smallrye"));

        return ops;
    }

    private void removeMicroProfileJWT(final List<ModelNode> ops, final PathAddress subsystem) {
        ops.add(createRemoveOperation(subsystem));
        ops.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.jwt-smallrye")));
    }

    private void adjustUndertow(final List<ModelNode> ops, final PathAddress subsystem) {
        // EAP 7.0 and earlier required explicit SSL configuration. Wildfly 10.1 added support
        // for SSL by default, which automatically generates certs.
        // This could be removed if all hosts were configured to contain a security domain with SSL enabled.
        // However, for the mixed domain tests, we are using a reduced host slave configuration file (see slave-config resource dir)
        // these configurations do not configure a SSL on ApplicationRealm, hence this removal to make it compatible across all domains.
        final PathAddress httpsListener = subsystem
                .append("server", "default-server")
                .append("https-listener", "https");
        ops.add(Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, httpsListener.toModelNode()));
    }

    private void adjustOpenTracing(final List<ModelNode> ops, final PathAddress subsystem) {
        // jaeger resource does not exist
        ops.add(createRemoveOperation(subsystem.append("jaeger-tracer", "jaeger")));
        ModelNode undefineAttr = createEmptyOperation("undefine-attribute", subsystem);
        undefineAttr.get("name").set("default-tracer");
        ops.add(undefineAttr);
    }
}
