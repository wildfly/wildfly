/*
 * Copyright 2016 Red Hat, Inc.
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

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Describes the associated job XML descriptors and the job name the XML descriptor is associated with.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class DeploymentJobDescriptors {
    private final Map<String, String> jobXmlDescriptors;
    private final Map<String, Set<String>> jobNames;

    DeploymentJobDescriptors() {
        jobXmlDescriptors = new ConcurrentSkipListMap<>();
        jobNames = new ConcurrentSkipListMap<>();
    }

    /**
     * Adds the job XML descriptor name and the job name to the collection of descriptors.
     *
     * @param jobXml  the job XML descriptor name
     * @param jobName the name of the job in the job XML
     */
    void add(final String jobXml, final String jobName) {
        jobXmlDescriptors.put(jobXml, jobName);
        final Set<String> xmlDescriptors = jobNames.computeIfAbsent(jobName, s -> {
            Set<String> result = new ConcurrentSkipListSet<>();
            final Set<String> appearing = jobNames.putIfAbsent(jobName, result);
            if (appearing != null) {
                result = appearing;
            }
            return result;
        });
        xmlDescriptors.add(jobXml);
    }

    /**
     * Returns all the job names associated with this deployment.
     *
     * @return the job names
     */
    Set<String> getJobNames() {
        return Collections.unmodifiableSet(jobNames.keySet());
    }

    /**
     * Validates whether or not the job name exists for this deployment.
     *
     * @param jobName the job name to check
     *
     * @return {@code true} if the job exists, otherwise {@code false}
     */
    boolean isValidJobName(final String jobName) {
        return jobName.contains(jobName);
    }

    /**
     * Returns all the job XML descriptors associated with this deployment.
     *
     * @return the job XML descriptors
     */
    Set<String> getJobXmlNames() {
        return Collections.unmodifiableSet(jobXmlDescriptors.keySet());
    }

    /**
     * Returns the job XML descriptors associated with a job.
     *
     * @param jobName the job name to find the XML descriptors for
     *
     * @return the set of job XML descriptors the job can be run from
     */
    Set<String> getJobXmlNames(final String jobName) {
        if (jobNames.containsKey(jobName)) {
            return Collections.unmodifiableSet(jobNames.get(jobName));
        }
        return Collections.emptySet();
    }

    /**
     * Validates whether or not the job XML descriptor exists for this deployment.
     *
     * @param jobXmlName the job XML descriptor name
     *
     * @return {@code true} if the job XML descriptor exists for this deployment, otherwise {@code false}
     */
    boolean isValidJobXmlName(final String jobXmlName) {
        return jobXmlDescriptors.containsKey(jobXmlName);
    }
}
