package tech.bletchleypark;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.joda.time.DateTime;
import org.json.JSONObject;

import tech.bletchleypark.enums.DateTimeUnit;
import tech.bletchleypark.tools.DateTimeTools;

public class ConfigProviderManager {

    public static DateTime optConfigDateTime(String propertyName, boolean plus) {
        Integer value = optConfigInt(propertyName + ".seconds", null);
        if (value != null)
            return DateTimeTools.getDateTime(value * (plus ? 1 : -1), DateTimeUnit.SECONDS);

        value = optConfigInt(propertyName + ".hours", null);
        if (value != null)
            return DateTimeTools.getDateTime(value * (plus ? 1 : -1), DateTimeUnit.HOURS);

        value = optConfigInt(propertyName + ".days", null);
        if (value != null)
            return DateTimeTools.getDateTime(value * (plus ? 1 : -1), DateTimeUnit.DAYS);

        value = optConfigInt(propertyName + ".weeeks", null);
        if (value != null)
            return DateTimeTools.getDateTime(value * (plus ? 1 : -1), DateTimeUnit.WEEKS);

        value = optConfigInt(propertyName + ".months", null);
        if (value != null)
            return DateTimeTools.getDateTime(value * (plus ? 1 : -1), DateTimeUnit.MONTHS);

        value = optConfigInt(propertyName + ".years", null);
        if (value != null)
            return DateTimeTools.getDateTime(value * (plus ? 1 : -1), DateTimeUnit.YEARS);

        return null;
    }

    public static int getConfigInt(String propertyName) {
        if (System.getenv(propertyName) != null) {
            return Integer.parseInt(System.getenv(propertyName));
        }
        return ConfigProvider.getConfig().getValue(propertyName, Integer.class);
    }

    public static Integer optConfigInt(String propertyName, Integer defaultInteger) {
        if (System.getenv(propertyName) != null) {
            return Integer.parseInt(System.getenv(propertyName));
        }
        return ConfigProvider.getConfig().getOptionalValue(propertyName, Integer.class).orElse(defaultInteger);
    }

    public static List<String> getConfigList(String propertyName) {
        return Arrays.asList(getConfigString(propertyName).split(","));
    }

    public static String getConfigString(String propertyName) {
        if (System.getenv(propertyName) != null) {
            return System.getenv(propertyName);
        }
        return ConfigProvider.getConfig().getValue(propertyName, String.class);
    }

    public static String optConfigString(String propertyName) {
        if (System.getenv(propertyName) != null) {
            return System.getenv(propertyName);
        }
        return optConfigString(propertyName, null);
    }

    public static String optConfigString(String propertyName, String defaultValue) {
        if (System.getenv(propertyName) != null) {
            return System.getenv(propertyName);
        }
        Optional<String> value = ConfigProvider.getConfig().getOptionalValue(propertyName, String.class);
        return value.isPresent() ? value.get() : defaultValue;
    }

    public static String configFetchPassword(String propertyName) {
        if (System.getenv(propertyName) != null) {
            return System.getenv(propertyName);
        }
        return ConfigProvider.getConfig().getValue(propertyName, String.class);
    }

    public static boolean getConfigBoolean(String propertyName) {
        if (System.getenv(propertyName) != null) {
            return System.getenv(propertyName).equalsIgnoreCase("true");
        }
        return ConfigProvider.getConfig().getValue(propertyName, Boolean.class);
    }

    public static boolean optConfigBoolean(String propertyName, boolean defaultValue) {
        if (System.getenv(propertyName) != null) {
            return System.getenv(propertyName).equalsIgnoreCase("true");
        }
        return ConfigProvider.getConfig().getOptionalValue(propertyName, Boolean.class).orElse(defaultValue);
    }

    public static boolean configHas(String propertyName) {
        return ConfigProvider.getConfig().getConfigValue(propertyName).getValue() != null;
    }

    public static String configFetch() {
        return configFetch(null, null);
    }

    public static String configFetch(String filter, String format) {
        return format != null && format.equalsIgnoreCase("json") ? configFetchAsJSONObject(filter).toString()
                : configFetchAsRaw(filter);
    }

    static List<String> keysToIgnore = Arrays.asList(new String[] {
            "formio",
            "",
            "xpc",
            "vscode",
            "vertx",
            "term",
            "sun", "ssl",
            "ssh",
            "original",
            "maven",
            "line",
            "io",
            "java",
            "maven_cmd_line_args",
            "colorterm",
            "command_mode",
            "curl_ca_bundle",
            "gdal_data",
            "git_askpass",
            "maven_projectbasedir",
            "mallocnanozone",
            "shell",
            "shlvl",
            "ssh_auth_sock",
            "ssl_cert_file",
            "term_program",
            "term_program_version",
            "vertxweb",
            "socksnonproxyhosts" });

    public static String configFetchAsRaw(String filter) {
        String result = "";
        Iterable<String> parameters = ConfigProvider.getConfig().getPropertyNames();
        HashMap<String, String> properties = new HashMap<>();
        parameters.forEach(key -> {
            try {
                properties.put(key, getConfigString(key));
            } catch (Exception e) {
            }
        });
        List<String> k = new ArrayList<>(properties.keySet());
        Collections.sort(k);
        for (String key : k) {
            String[] parts = key.split("\\.");
            if (!keysToIgnore.contains(parts[0].toLowerCase()))
                if (filter == null
                        || filter.isEmpty()
                        || key.toLowerCase().startsWith(filter.toLowerCase()))
                    result += (result.isEmpty() ? "" : "\n") + key + "=" + properties.get(key);
        }
        return result;
    }

    public static JSONObject configFetchAsJSONObject(String filter) {
        JSONObject result = new JSONObject();
        Iterable<String> parameters = ConfigProvider.getConfig().getPropertyNames();
        parameters.forEach(key -> {
            if (key == null || key.startsWith(filter)) {
                try {
                    String[] path = key.split("\\.");
                    if (path.length == 1) {
                        result.put(key, getConfigString(key));
                    } else {
                        JSONObject json;
                        if (!result.has(path[0])) {
                            json = new JSONObject();
                            result.put(path[0], new JSONObject());
                        }
                        json = result.getJSONObject(path[0]);
                        for (int idx = 1; idx < path.length - 1; idx++) {
                            if (!json.has(path[idx])) {
                                json.put(path[idx], new JSONObject());
                            }
                            json = json.getJSONObject(path[idx]);
                        }
                        if (parameters.toString().contains(key + ".")) {
                            json.put(path[path.length - 1], new JSONObject());
                            json = json.getJSONObject(path[path.length - 1]);
                        }
                        json.put(path[path.length - 1], getConfigString(key));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    result.put(key, e.getMessage());
                }
            }
        });

        return result;
    }

    public static Path getConfigPath(String propertyName) {
        try {
            return getConfigPath(propertyName, true);
        } catch (IOException e) {
            return null;
        }
    }

    public static Path getConfigPath(String propertyName, boolean createPath) throws IOException {
        Path path = Paths.get(ConfigProvider.getConfig().getValue(propertyName, String.class));
        if (!createPath || Files.exists(path)) {
            return path;
        }
        Files.createDirectories(path);
        return path;
    }

    public static String[] configFetchStringArray(String string) {
        return getConfigString(string).split(",");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T configFetchEnum(String enumValue, Class<T> enumClass) {
        try {
            return Enum.valueOf(enumClass, enumValue.toUpperCase());
        } catch (Exception ex) {
        }
        Method[] methods = enumClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals("defaultValue")) {
                try {
                    return (T) method.invoke(null);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static JSONObject getConfig(String startsWith) {
        JSONObject json = new JSONObject();
        ConfigProvider.getConfig().getConfigSources().forEach(s -> {
            s.getProperties().keySet().forEach(key -> {
                if (key.startsWith(startsWith)) {
                    if (!key.contains("password")
                            || optConfigBoolean("global.show.password", false)
                            || key.equals("global.show.password")) {
                        json.put(key, optConfigString(key, "ERROR"));
                    }
                }
            });
        });

        return json;
    }

}
