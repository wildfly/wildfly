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

import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
public class ReactiveStreamsOperatorsInjectedReactiveEngineProviderSanityTestCase {

    @Inject
    ReactiveStreamsEngine engine;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class,  "rx-stream-ops.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClass(ReactiveStreamsOperatorsInjectedReactiveEngineProviderSanityTestCase.class)
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new FilePermission("<<ALL FILES>>", "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    public void testReactiveApiWithInjectedEngine() throws Exception {
        Assert.assertNotNull(engine);

        CompletionStage<List<String>> cs = ReactiveStreams.of("this", "is", "only", "a", "test")
                .map(s -> s.toUpperCase(Locale.ENGLISH)) // Transform the words
                .filter(s -> s.length() > 3) // Filter items
                .collect(Collectors.toList())
                .run(engine);

        List<String> result = cs.toCompletableFuture().get();

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("THIS", result.get(0));
        Assert.assertEquals("ONLY", result.get(1));
        Assert.assertEquals("TEST", result.get(2));
    }
}
