package org.jboss.as.mail.extension;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.Properties;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @created 25.7.11 15:48
 */
public class MailSessionConfig {
    private static final Logger log = Logger.getLogger(MailSessionConfig.class);
    private String jndiName;
    private String username;
    private String password;
    private String smtpServerAddress;
    private String smtpServerPort;
    //private Properties properties = new Properties();



    protected MailSessionConfig() {

    }

    public String getJndiName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /*public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }*/

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSmtpServerAddress() {
        return smtpServerAddress;
    }

    public void setSmtpServerAddress(String smtpServerAddress) {
        this.smtpServerAddress = smtpServerAddress;
    }

    public String getSmtpServerPort() {
        return smtpServerPort;
    }

    public void setSmtpServerPort(String smtpServerPort) {
        this.smtpServerPort = smtpServerPort;
    }
    /*public void setSMTPServerAddress(String address){
        properties.put()
    }
    public void setSMTPServerPort(String port){

    }*/

    @Override
    public String toString() {
        return "MailSessionConfig{" +
                "jndiName='" + jndiName + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", smtpServerAddress='" + smtpServerAddress + '\'' +
                ", smtpServerPort='" + smtpServerPort + '\'' +
                '}';
    }
}
