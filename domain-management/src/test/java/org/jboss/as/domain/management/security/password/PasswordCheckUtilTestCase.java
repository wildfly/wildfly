package org.jboss.as.domain.management.security.password;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.as.domain.management.security.adduser.AddUser;

/**
 * @author <a href="mailto:g.grossetie@gmail.com">Guillaume Grossetie</a>
 */
public class PasswordCheckUtilTestCase {

    @Test
    public void testInitRestriction() throws URISyntaxException {
        final URL resource = PasswordCheckUtilTestCase.class.getResource("add-user.properties");
        File configFile = new File(resource.toURI());
        System.setProperty(AddUser.CONFIG_FILE, configFile.getAbsolutePath());
        final List<PasswordRestriction> passwordRestrictions = new PasswordCheckUtil().getPasswordRestrictions("ggrossetie");
        assertPasswordRejected(passwordRestrictions, "ggrossetie", "Password must not match username");
        assertPasswordRejected(passwordRestrictions, "abc12", "Password must have at least 8 characters");
        assertPasswordRejected(passwordRestrictions, "abcdefgh", "Password must have at least 2 digits");
        assertPasswordRejected(passwordRestrictions, "abcdefgh1", "Password must have at least 2 digits");
        assertPasswordRejected(passwordRestrictions, "root", "Password must not be 'root'");
        assertPasswordRejected(passwordRestrictions, "admin", "Password must not be 'admin'");
        assertPasswordRejected(passwordRestrictions, "administrator", "Password must not be 'administrator'");
        assertPasswordAccepted(passwordRestrictions, "abcdefgh12", "Password is valid");
    }

    private void assertPasswordRejected(List<PasswordRestriction> passwordRestrictions, String password, String expectedMessage) {
        boolean accepted = true;
        for(PasswordRestriction passwordRestriction : passwordRestrictions) {
            accepted &= passwordRestriction.pass(password);
        }
        assertFalse(expectedMessage, accepted);
    }

    private void assertPasswordAccepted(List<PasswordRestriction> passwordRestrictions, String password, String expectedMessage) {
        boolean accepted = true;
        for(PasswordRestriction passwordRestriction : passwordRestrictions) {
            accepted &= passwordRestriction.pass(password);
        }
        assertTrue(expectedMessage, accepted);
    }
}
