/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.shared;

import java.io.File;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepositories;
import org.jboss.shrinkwrap.resolver.api.maven.repository.MavenRemoteRepository;

/**
 * A simple utility used to retrieve libraries for tests from Maven. If the artifact does not exist locally, it will
 * be downloaded.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class MavenArtifactResolver {

    /**
     * Resolves the Maven artifact and returns the artifact file associated with the coordinates.
     *
     * @param mavenCoords the maven coordinates
     *
     * @return the artifact file
     */
    public static File resolveSingleFile(final String mavenCoords) {
        return createResolver("pom.xml")
                .resolve(mavenCoords)
                .withTransitivity()
                .asSingleFile();
    }

    /**
     * Resolves the Maven artifacts and returns the artifact files associated with the coordinates.
     *
     * @param mavenCoords the maven coordinates
     *
     * @return the artifact files
     */
    public static File[] resolve(final String... mavenCoords) {
        return createResolver("pom.xml")
                .resolve(mavenCoords)
                .withoutTransitivity()
                .asFile();
    }

    /**
     * Resolves the Maven artifacts and returns the artifact files associated with the coordinates. If
     * {@code withTransitivity} is set to {@code true}, transitive dependencies will also be returned.
     *
     * @param withTransitivity {@code true} if transitive dependencies should also be returned
     * @param mavenCoords      the maven coordinates
     *
     * @return the artifact files
     */
    public static File[] resolve(final boolean withTransitivity, final String... mavenCoords) {
        if (withTransitivity) {
            return createResolver("pom.xml")
                    .resolve(mavenCoords)
                    .withTransitivity()
                    .asFile();
        }
        return resolve(mavenCoords);
    }

    private static PomEquippedResolveStage createResolver(final String pomFile) {
        final var value = System.getProperty("org.jboss.model.test.maven.repository.urls");
        if (value == null) {
            return Maven.resolver().loadPomFromFile(pomFile);
        }
        var resolverSystem = Maven.configureResolver()
                .withMavenCentralRepo(false);
        final var repos = value.split(",");
        for (var i = 0; i < repos.length; i++) {
            final MavenRemoteRepository repo = MavenRemoteRepositories.createRemoteRepository("wildfly-custom-" + i, repos[i], "default");
            resolverSystem = resolverSystem.withRemoteRepo(repo);
        }
        return resolverSystem
                .loadPomFromFile(pomFile);
    }
}
