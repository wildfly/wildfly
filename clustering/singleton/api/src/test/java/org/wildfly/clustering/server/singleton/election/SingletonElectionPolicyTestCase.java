/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.singleton.election;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionPolicyTestCase {

    @Test
    public void test() {
        GroupMember member1 = mock(GroupMember.class);
        GroupMember member2 = mock(GroupMember.class);
        GroupMember member3 = mock(GroupMember.class);

        List<GroupMember> candidates = List.of(member1, member2, member3);

        Assert.assertSame(member1, SingletonElectionPolicy.oldest().elect(candidates));
        Assert.assertSame(member2, SingletonElectionPolicy.position(1).elect(candidates));
        Assert.assertSame(member3, SingletonElectionPolicy.youngest().elect(candidates));
        Assert.assertNotNull(SingletonElectionPolicy.random().elect(candidates));

        doReturn("member1").when(member1).getName();
        doReturn("member2").when(member2).getName();
        doReturn("member3").when(member3).getName();

        Assert.assertSame(member1, SingletonElectionPolicy.youngest().prefer(List.of(prefer("member1"))).elect(candidates));
        Assert.assertSame(member1, SingletonElectionPolicy.youngest().prefer(List.of(prefer("member1"), prefer("member2"))).elect(candidates));
        Assert.assertSame(member2, SingletonElectionPolicy.youngest().prefer(List.of(prefer(Set.of("member1", "member2")))).elect(candidates));
        Assert.assertSame(member2, SingletonElectionPolicy.youngest().prefer(List.of(prefer(Set.of("member1", "member2")), prefer(Set.of("member2", "member3")))).elect(candidates));
        Assert.assertSame(member3, SingletonElectionPolicy.youngest().prefer(List.of(prefer("member4"))).elect(candidates));
    }

    private static Predicate<GroupMember> prefer(String name) {
        return member -> name.equals(member.getName());
    }

    private static Predicate<GroupMember> prefer(Set<String> names) {
        return member -> names.contains(member.getName());
    }
}
