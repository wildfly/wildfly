/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.streams.operators;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
public class ReactiveStreamsOperatorsNoReactiveEngineProviderSanityTestCase {

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class,  "rx-stream-ops.war")
                .addClass(ReactiveStreamsOperatorsNoReactiveEngineProviderSanityTestCase.class)
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new FilePermission("<<ALL FILES>>", "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    public void testReactiveApi() throws Exception {
        CompletionStage<List<String>> cs = ReactiveStreams.of("this", "is", "only", "a", "test")
                .map(s -> s.toUpperCase(Locale.ENGLISH)) // Transform the words
                .filter(s -> s.length() > 3) // Filter items
                .collect(Collectors.toList())
                .run();

        List<String> result = cs.toCompletableFuture().get();

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("THIS", result.get(0));
        Assertions.assertEquals("ONLY", result.get(1));
        Assertions.assertEquals("TEST", result.get(2));
    }
}
