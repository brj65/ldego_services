package lde.kiwi.mfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONObject;

import tech.bletchleypark.beans.User;

public class MFilesUsers {

    public static void doImport(Path filePath) throws IOException, SQLException {
        List<String> lines = Files.readAllLines(filePath);
        for (int index = 1; index < lines.size(); index++) {
            String[] data = lines.get(index).split("\t");
            String login = data[0].contains("\\") ? data[0].substring(data[0].indexOf("\\")+1) : data[0];
            int id = Integer.parseInt(data[1]);
            String name = data[2];
            String firstName;
            String lastName; 
             if (name.contains(" ")) {
                 firstName = name.substring(0, name.indexOf(" ")).trim();
                 lastName = name.substring(name.indexOf(" ")).trim();
            } else {
                 firstName = name;
                 lastName = "";
            }
            boolean disabled = data[3].equals("Disabled");
            String type = data[4];
            JSONObject other = new JSONObject();
            JSONObject mfiles = new JSONObject();
            mfiles.put("id", id);
            mfiles.put("type", type);
            other.put("mfiles", mfiles);
            User user = User.fetch(login);
            if (user == null) {
                user = new User(login, firstName, lastName, disabled, other);
            } else {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setDisabled(disabled);
                user.addOther(other);
            }
            user.update();
        }

    }

}
