package org.jboss.as.domain.management.security;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import junit.framework.Assert;

import org.junit.Test;

public class LdapSubjectSupplementalTestCase {


    @Test
    public void testLdapRole() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        TestLdapSubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, null, 0, null);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmRole("cn=admin,ou=roles,dc=jboss,dc=org")));
        Assert.assertTrue(principals.contains(new RealmRole("cn=emeaadmin,ou=roles,dc=jboss,dc=org")));
        Assert.assertEquals(3, principals.size());
    }

    @Test
    public void testLdapRolePattern() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        TestLdapSubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null, "", null, "=([^,]+)", 0, null);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmRole("=admin")));
        Assert.assertTrue(principals.contains(new RealmRole("=emeaadmin")));
        Assert.assertEquals(3, principals.size());
    }

    @Test
    public void testLdapRoleGroup() throws IOException {
        RealmUser realmUser = new RealmUser("TestRealm", "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);
        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        TestLdapSubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, "=([^,]+)", 1, null);
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmRole("admin")));
        Assert.assertTrue(principals.contains(new RealmRole("emeaadmin")));
        Assert.assertEquals(3, principals.size());

        //Test with a empty regular expressions
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "", 1, null);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

      //Test with a rubbish regular expressions
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "{[]]]]]", 1, null);
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

      //Test with a group higher then the actual groups returned by the regular expressions
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null, "", null, "=([^,]+)", 9, null);
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
        TestLdapSubjectSupplemental subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null, "", null, "=([^,]+)", 1, "{0}-{2}");
        subjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmRole("admin-jboss")));
        Assert.assertTrue(principals.contains(new RealmRole("emeaadmin-jboss")));
        Assert.assertEquals(3, principals.size());

        //test with empty result pattern
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "=([^,]+)", 1, "");
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

        //test with one result pattern
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, "=([^,]+)", 1, "{0}");
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmRole("admin")));
        Assert.assertTrue(principals.contains(new RealmRole("emeaadmin")));
        Assert.assertEquals(3, principals.size());

        //test with result pattern that use a higher number of elements
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null, null,"", null, "=([^,]+)", 1, "{9}");
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());

      //test with result pattern with fixed pattern
        subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        subjectSupplemental = new TestLdapSubjectSupplemental(false, null, null,null, "", null, "=([^,]+)", 1, "test");
        subjectSupplemental.supplementSubject(subject);
        principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());
    }


    private class TestLdapSubjectSupplemental extends LdapSubjectSupplemental {
        public TestLdapSubjectSupplemental(boolean recursive, String rolesDn, String baseDn, String userDn, String userNameAttribute, String advancedFilter, String pattern, int groups, String resultPattern) {
            super(recursive,rolesDn,baseDn,userDn,userNameAttribute,advancedFilter,pattern,groups,resultPattern,true);
        }

        @Override
        protected Set<String> searchLdap(String username) {
            HashSet<String> ldapRoles = new HashSet<String>();
            ldapRoles.add("cn=admin,ou=roles,dc=jboss,dc=org");
            ldapRoles.add("cn=emeaadmin,ou=roles,dc=jboss,dc=org");
            return ldapRoles;
        }
    }

}
