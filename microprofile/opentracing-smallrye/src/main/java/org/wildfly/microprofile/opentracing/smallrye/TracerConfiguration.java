/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.microprofile.opentracing.smallrye;

import io.opentracing.Tracer;
import org.jboss.dmr.ModelNode;

/**
 * Interface wrapping every tracer implementation configuration.
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public interface TracerConfiguration {

    /**
     *  Returns this configuration name.
     * @return this configuration name.
     */
    String getName();

    /**
     * Instantiate a new Tracer
     * @param serviceName: service name associated with this tracer.
     * @return a new tracer instance.
     */
    Tracer createTracer(String serviceName);

    /**
     * The JBoss module required to instantiate the tracer.
     * @return the JBoss module required to instantiate the tracer.
     */
    String getModuleName();

    /**
     * Returns the description of the tracer configuration.
     * @return the configuration used at tracer's instantiation.
     */
    ModelNode getModel();
}
