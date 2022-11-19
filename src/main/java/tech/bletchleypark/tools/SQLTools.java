package tech.bletchleypark.tools;

import java.sql.Connection;
import java.sql.SQLException;

import io.agroal.api.AgroalDataSource;

public class SQLTools {

    public static Connection getConnection(AgroalDataSource defaultDataSource) {
        Connection connection = null;
        do {
            try {
                connection = defaultDataSource.getConnection();
            } catch (Exception e) {
                try {
                    connection.close();
                } catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                e.printStackTrace();
                GeneralTools.pause(.5);
            }
        } while (connection == null);
        return connection;
    }
}
