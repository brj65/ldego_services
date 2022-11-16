package tech.bletchleypark.beans;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.joda.time.DateTime;

public class User {
    private String login;
    private int extension;
    private int pin;
    private String firstName;
    private String lastName;
    private DateTime lastupdated;

    public User(ResultSet rst) throws SQLException {
        login = rst.getString("login");
        extension = rst.getInt("extension");
        pin = rst.getInt("pin");
        firstName = rst.getString("first_name");
        lastName = rst.getString("last_name");
        lastupdated = new DateTime(rst.getTimestamp("lastupdated"));
    }

    public String getLogin() {
        return login;
    }

    public int getExtension() {
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

  
public org.json.JSONObject JSONObject(){
    org.json.JSONObject json = new org.json.JSONObject();
    json.put("login",login);
    json.put("extension",extension);
    json.put("pin",pin);
    json.put("first_name",firstName);
    json.put("last_name",lastName);
    json.put("lastupdated",lastupdated);
    return json;
}
}
