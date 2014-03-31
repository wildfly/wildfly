package org.wildfly.test.integration.security.picketlink.idm.entities;

import org.picketlink.idm.jpa.annotations.AttributeValue;
import org.picketlink.idm.jpa.annotations.entity.IdentityManaged;
import org.picketlink.idm.model.annotation.Unique;
import org.picketlink.idm.model.basic.User;

import javax.persistence.Entity;

/**
 * Created with IntelliJ IDEA. User: pedroigor Date: 8/26/13 Time: 10:13 AM To change this template use File | Settings | File Templates.
 */
@IdentityManaged(User.class)
@Entity
public class AccountEntity extends IdentityTypeEntity {

    @Unique
    @AttributeValue
    private String loginName;

    @AttributeValue
    private String firstName;

    @AttributeValue
    private String lastName;

    @AttributeValue
    private String email;

    public String getLoginName() {
        return this.loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
