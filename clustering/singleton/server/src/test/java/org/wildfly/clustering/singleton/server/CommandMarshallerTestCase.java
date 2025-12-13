/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.List;

import org.infinispan.remoting.transport.Address;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.EmbeddedCacheManagerGroupMember;

/**
 * @author Paul Ferraro
 */
public class CommandMarshallerTestCase {

    @ParameterizedTest
    @TesterFactorySource({ MarshallingTesterFactory.class })
    public void test(TesterFactory factory) {
        factory.createTester().accept(StartCommand.INSTANCE);
        factory.createTester().accept(StopCommand.INSTANCE);
        factory.createTester().accept(PrimaryProviderCommand.INSTANCE);
        factory.createTester(Assertions::assertSame).accept(SingletonValueCommand.INSTANCE);
        CacheContainerGroupMember elected = createMember();
        factory.createTester(CommandMarshallerTestCase::assertEquals).accept(new SingletonElectionCommand(List.of(createMember(), elected, createMember()), elected));
    }

    private static CacheContainerGroupMember createMember() {
        return new EmbeddedCacheManagerGroupMember(Address.random());
    }

    private static void assertEquals(SingletonElectionCommand command1, SingletonElectionCommand command2) {
        Assertions.assertEquals(command1.getCandidates(), command2.getCandidates());
        Assertions.assertEquals(command1.getIndex(), command2.getIndex());
    }
}
