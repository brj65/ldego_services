package tech.bletchleypark.beans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import tech.bletchleypark.ApplicationLifecycle;

public class User {
    private String login;
    private String extension;
    private int pin;
    private String firstName;
    private String lastName;
    private String email = "";
    private String mobile = "";
    private JSONObject other = new JSONObject();
    private boolean disabled = false;
    private DateTime lastupdated;

    public User(String login, String firstName, String lastName, boolean disabled, JSONObject other) {
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.disabled = disabled;
        addOther(other);

    }

    public User(ResultSet rst) throws SQLException {
        login = rst.getString("login");
        extension = rst.getString("extension");
        pin = rst.getInt("pin");
        firstName = rst.getString("first_name");
        lastName = rst.getString("last_name");
        email = rst.getString("email");
        mobile = rst.getString("mobile");
        disabled = rst.getBoolean("disabled");
        lastupdated = new DateTime(rst.getTimestamp("lastupdated"));
        try {
            other = new JSONObject(rst.getString("other"));
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public JSONObject getOther() {
        return other;
    }

    public void setOther(JSONObject other) {
        this.other = other;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setLastupdated(DateTime lastupdated) {
        this.lastupdated = lastupdated;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public User addOther(JSONObject other) {
        other.keySet().forEach(key -> {
            if (this.other.has(key)) {
                this.other.remove(key);
            }
            this.other.put(key, other.getJSONObject(key));
        });
        return this;
    }

    public String getLogin() {
        return login;
    }

    public String getExtension() {
        return extension;
    }

    public int getPin() {
        return pin;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public DateTime getLastupdated() {
        return lastupdated;
    }

    public org.json.JSONObject JSONObject() {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("login", login);
        json.put("disabled", disabled);
        json.put("extension", extension);
        json.put("pin", pin);
        json.put("first_name", firstName);
        json.put("last_name", lastName);
        json.put("email", email);
        json.put("mobile", mobile);
        json.put("other", other);
        json.put("lastupdated", lastupdated);
        return json;
    }

    public void update() throws SQLException {
        User user = fetch(login);
        if (user == null) {
            try (Connection connection = ApplicationLifecycle.application.defaultDataSource.getConnection();
                    PreparedStatement ps = connection.prepareStatement("INSERT INTO `user` " +
                            "(login,disabled,extension,pin,first_name,last_name,other,email,mobile) "
                            + "VALUES (?,?,?,?,?,?,?,?,?)");) {
                int idx = 1;
                ps.setString(idx++, login);
                ps.setBoolean(idx++, disabled);
                ps.setString(idx++, extension);
                ps.setInt(idx++, pin);
                ps.setString(idx++, firstName);
                ps.setString(idx++, lastName);
                ps.setString(idx++, other.toString());
                ps.setString(idx++, email);
                ps.setString(idx++, mobile);
                int x = ps.executeUpdate();
                if (x != 1) {
                    int a = 0;
                }
            }
        } else {
            try (Connection connection = ApplicationLifecycle.application.defaultDataSource.getConnection();
                    PreparedStatement ps = connection.prepareStatement("UPDATE `user` SET " +
                            "disabled = ?,extension = ?,pin = ?,first_name = ?,last_name = ?,other = ?,email = ?,mobile = ? "
                            + " WHERE login = ?");) {
                int idx = 1;

                ps.setBoolean(idx++, disabled);
                ps.setString(idx++, extension);
                ps.setInt(idx++, pin);
                ps.setString(idx++, firstName);
                ps.setString(idx++, lastName);
                ps.setString(idx++, other.toString());
                ps.setString(idx++, email);
                ps.setString(idx++, mobile==null?"":mobile);
                // key
                ps.setString(idx++, login);
                int x = ps.executeUpdate();
                if (x != 1) {
                    int a = 0;
                }
            }
        }
    }

    public static User fetch(String login) throws SQLException {
        User user = null;
        try (Connection connection = ApplicationLifecycle.application.defaultDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement("SELECT * FROM user WHERE login = ?");) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery();) {
                if (rs.next()) {
                    user = new User(rs);
                }
            }
        }
        return user;
    }

    public static User fetch(String firstName, String lastName) throws SQLException {
        User user = null;
        try (Connection connection = ApplicationLifecycle.application.defaultDataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM user WHERE LOWER(first_name) = ?  AND LOWER(last_name) = ?");) {
            ps.setString(1, firstName.toLowerCase());
            ps.setString(2, lastName.toLowerCase());
            try (ResultSet rs = ps.executeQuery();) {
                if (rs.next()) {
                    user = new User(rs);
                }
            }
        }
        return user;
    }

}
