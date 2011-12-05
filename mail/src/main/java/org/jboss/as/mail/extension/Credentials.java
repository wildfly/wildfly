package org.jboss.as.mail.extension;

/**
 * @author Tomaz Cerar
 * @created 22.8.11 11:50
 */
class Credentials {
    private final String username;
    private final String password;

    public Credentials(String username, String password) {
        this.password = password;
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }
}
