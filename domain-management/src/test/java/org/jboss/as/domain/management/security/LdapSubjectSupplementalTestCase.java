/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.junit.Assert;

import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.domain.management.security.LdapSubjectSupplementalService.LdapSubjectSupplemental;
import org.junit.Test;

public class LdapSubjectSupplementalTestCase {

    private static Map<String, Object> sharedState = Collections.emptyMap();

    @Test
    public void testLdapRole() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        SubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, null, 0, null).getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmGroup("cn=admin,ou=groups,dc=jboss,dc=org")));
        Assert.assertTrue(principals.contains(new RealmGroup("cn=emeaadmin,ou=groups,dc=jboss,dc=org")));
        Assert.assertEquals(3, principals.size());
    }

    @Test
    public void testLdapRolePattern() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        SubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null, "", null, "=([^,]+)", 0, null).getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmGroup("=admin")));
        Assert.assertTrue(principals.contains(new RealmGroup("=emeaadmin")));
        Assert.assertEquals(3, principals.size());
    }

    @Test
    public void testLdapRoleGroup() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        SubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, "=([^,]+)", 1, null).getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmGroup("admin")));
        Assert.assertTrue(principals.contains(new RealmGroup("emeaadmin")));
        Assert.assertEquals(3, principals.size());

        //Test with a empty regular expressions
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "", 1, null).getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

      //Test with a rubbish regular expressions
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "{[]]]]]", 1, null).getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

      //Test with a group higher then the actual groups returned by the regular expressions
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null, "", null, "=([^,]+)", 9, null).getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());
    }

    @Test
    public void testLdapRoleResultPattern() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        SubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null, "", null, "=([^,]+)", 1, "{0}-{2}").getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmGroup("admin-jboss")));
        Assert.assertTrue(principals.contains(new RealmGroup("emeaadmin-jboss")));
        Assert.assertEquals(3, principals.size());

        //test with empty result pattern
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "=([^,]+)", 1, "").getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

        //test with one result pattern
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, "=([^,]+)", 1, "{0}").getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmGroup("admin")));
        Assert.assertTrue(principals.contains(new RealmGroup("emeaadmin")));
        Assert.assertEquals(3, principals.size());

        //test with result pattern that use a higher number of elements
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, "=([^,]+)", 1, "{9}").getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

      //test with result pattern with fixed pattern
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "=([^,]+)", 1, "test").getSubjectSupplemental(sharedState);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());
    }


    private class TestLdapSubjectSupplemental extends LdapSubjectSupplementalService {
        public TestLdapSubjectSupplemental(boolean recursive, String groupsDn, String baseDn, String userDn, String userNameAttribute, String advancedFilter, String pattern, int groups, String resultPattern) {
            super(recursive,groupsDn,baseDn,userDn,userNameAttribute,advancedFilter,pattern,groups,resultPattern,true, false);
        }

        @Override
        public SubjectSupplemental getSubjectSupplemental(Map<String, Object> sharedState) {
            // TODO Auto-generated method stub
            return new LdapSubjectSupplemental(sharedState) {
                @Override
                protected Set<String> searchLdap(String username) {
                    HashSet<String> ldapRoles = new HashSet<String>();
                    ldapRoles.add("cn=admin,ou=groups,dc=jboss,dc=org");
                    ldapRoles.add("cn=emeaadmin,ou=groups,dc=jboss,dc=org");
                    return ldapRoles;
                }
            };
        }

    }

}
