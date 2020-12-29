package sample;

public class User {
    private String fname;
    private String lname;
    private String login;
    private String password;
    private String token;


    public User(String fname, String lname, String login, String password) {
        this.fname = fname;
        this.lname = lname;
        this.login = login;
        this.password = password;
        this.token = null;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}