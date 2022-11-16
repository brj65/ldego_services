package tech.bletchleypark;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import static tech.bletchleypark.ConfigProviderManager.*;

@ApplicationScoped
public class ApplicationLifecycle {
    public static ApplicationLifecycle application;

    @Inject
    public AgroalDataSource defaultDataSource;

    public String test() {
        return "test";
    }

    public boolean shhRequest(String passcode) {
        return passcode != null && optConfigString("bpark.shh.passcode", "Guessthispwd").equals(passcode);
    }

    public int getInstanceId() {
        return getConfigInt("temp.id");
    }

    public void onStart(@Observes StartupEvent ev) {
        application = this;
    }

    public JSONObject getDefaultDataSourceConfig() {
        JSONObject dsc = new JSONObject();
        dsc.put("db-kind", optConfigString("quarkus.datasource.db-kind", ""));
        dsc.put("username", optConfigString("quarkus.datasource.username", ""));
        dsc.put("password", optConfigString("quarkus.datasource.password", ""));
        dsc.put("url", optConfigString("quarkus.datasource.jdbc.url", ""));
        dsc.put("leak-detection-interval", optConfigString("quarkus.datasource.jdbc.leak-detection-interval", ""));
        dsc.put("enable-metrics", optConfigString("quarkus.datasource.jdbc.enable-metrics", ""));
        dsc.put("mix_size", optConfigString("quarkus.datasource.jdbc.min-size", ""));
        dsc.put("max_size", optConfigString("quarkus.datasource.jdbc.max-size", ""));
        dsc.put("inital_size", optConfigString("quarkus.datasource.jdbc.initial-size", ""));
        dsc.put("new-connection-sql", optConfigString("quarkus.datasource.jdbc.new-connection-sql", ""));
        dsc.put("acquisition-timeout",optConfigString("quarkus.datasource.jdbc.acquisition-timeout",""));
        return dsc;
    }
    public JSONObject getConfig() {
        JSONObject config = new JSONObject();
        config.put("session_local_only", optConfigString("bpark.sessions.local.only", ""));
       
        return config;
    }

    public boolean localSessionsOnly(){
        return optConfigBoolean("bpark.sessions.local.only", false);
        
    }


    public JSONObject toJSONObject(String key,JSONArray array){
        JSONObject json = new JSONObject();
        json.put(key, array);
        return json;
    }
}
