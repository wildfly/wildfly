package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DATABASE_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_TABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERNAME_FIELD;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.UnsupportedCallbackException;

import junit.framework.Assert;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.connections.database.DatabaseConnectionManagerService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.junit.Test;

public class DatabaseSubjectSupplementalTestCase extends AbstractDatabaseConnectionTestHelper{

    private ModelNode cNode;
    private DatabaseSubjectSupplemental databaseSubjectSupplemental;

    @Override
    void initCallbackHandler(final DatabaseConnectionManagerService dcs) throws Exception {
        databaseSubjectSupplemental = new DatabaseSubjectSupplemental(TEST_REALM, cNode) {
            @Override
              public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
                  InjectedValue<ConnectionManager> cm = new InjectedValue<ConnectionManager>();
                  cm.setValue(new ImmediateValue<ConnectionManager>(dcs));
                  return cm;
              }
        };

    }

    @Override
    void initAuthenticationModel(boolean plainPassword) {
        cNode = new ModelNode();
        cNode.get(OP).set(ADD);
        cNode.get(OP_ADDR).add(DATABASE_CONNECTION, "db");
        cNode.get(PLAIN_TEXT).set(plainPassword);
        cNode.get(SIMPLE_SELECT_TABLE).set("roles");
        cNode.get(SIMPLE_SELECT_ROLES).set(true);
        cNode.get(SIMPLE_SELECT_USERNAME_FIELD).set("user");
        cNode.get(ROLES_FIELD).set("roles");
    }

    @Test
    public void testHandleRoles() throws IOException, UnsupportedCallbackException {
        RealmUser realmUser = new RealmUser(TEST_REALM, "Jack.Carter");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);

        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        databaseSubjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmRole("sheriff")));
        Assert.assertTrue(principals.contains(new RealmRole("dad")));
        Assert.assertTrue(principals.contains(new RealmRole("lifesaver")));
        Assert.assertTrue(principals.contains(new RealmUser(TEST_REALM,"Jack.Carter")));
        Assert.assertEquals(4, principals.size());
    }

    @Test
    public void testHandleNoRoles() throws IOException, UnsupportedCallbackException {
        RealmUser realmUser = new RealmUser(TEST_REALM, "Henry.Deacon");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);

        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        databaseSubjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertTrue(principals.contains(new RealmUser(TEST_REALM,"Henry.Deacon")));
        Assert.assertEquals(1, principals.size());
    }


    @Test
    public void testHandleBuggyData() throws IOException, UnsupportedCallbackException {
        RealmUser realmUser = new RealmUser(TEST_REALM, "Christopher.Chance");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);

        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        databaseSubjectSupplemental.supplementSubject(subject);
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertEquals(2, principals.size());
    }

    @Test
    public void testHandleNoUser() throws IOException, UnsupportedCallbackException {
        RealmUser realmUser = new RealmUser(TEST_REALM, "John.Doe");
        HashSet<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(realmUser);

        Subject subject = new Subject(false,principalSet,new HashSet<Object>(),new HashSet<Object>());
        try {
            databaseSubjectSupplemental.supplementSubject(subject);
            Assert.fail("Expect a UserNotFoundException was throwed");
        } catch (IOException e) {
            Assert.assertEquals(UserNotFoundException.class, e.getCause().getClass());
        }
        Set<Principal> principals = subject.getPrincipals();
        Assert.assertEquals(1, principals.size());
    }


}
