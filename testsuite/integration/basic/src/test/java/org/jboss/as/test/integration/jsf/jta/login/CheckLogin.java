package org.jboss.as.test.integration.jsf.jta.login;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.jboss.as.test.integration.jsf.jta.JTATestsBase;
import org.junit.Assert;

@ManagedBean
@SessionScoped
public class CheckLogin {


    String loginname;
    String password;
    
    public CheckLogin() {
        // TODO Auto-generated constructor stub
    }


    public String getLoginname() {
        return loginname;
    }

    public void setLoginname(String loginname) {
        this.loginname = loginname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String checkValidUser() {
        JTATestsBase.doLookupTest();
        if (loginname.equals("root") && password.equals("root")) {
            return "success";
        } else {
            return "fail";
        }
    }

}
