package com.yumegod.obfuscator.utils.cfg;

import com.yumegod.obfuscator.utils.EnumUtils;
import com.yumegod.obfuscator.utils.apache.StringUtils;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import com.yumegod.obfuscator.utils.reflections.ReflectionUtil;
import io.github.portlek.jsongration.JsonConfiguration;
import io.github.portlek.tomlgration.TomlConfiguration;
import org.objectweb.asm.Type;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.FileConfiguration;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ConfigManager {
    static FileConfiguration configuration;
    public static void loadConfig(File file) throws Exception {
        try {
            configuration = createConfiguration(file);
            configuration.setDefaults(defaultConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
            configuration = defaultConfiguration();
        }
        HashSet<String> classes = ReflectionUtil.getClassesByAnnotation(Type.getDescriptor(StaticConfigReceiver.class));
        for (String className : classes) {
            try {
                Class<?> clazz = Class.forName(className);
                StaticConfigReceiver annotation = clazz.getAnnotation(StaticConfigReceiver.class);
                String sectionPath = annotation.basicSection();
                ConfigurationSection section = configuration.getConfigurationSection(sectionPath);
                if (section == null) {
                    throw new IOException("Undefined config section: " + sectionPath);
                }
                loadConfig(clazz, section);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static FileConfiguration getConfig() {
        return configuration;
    }

    public static List<String> getStringList(String path) {
        if (configuration.isList(path)) return configuration.getStringList(path);
        return Collections.singletonList(configuration.getString(path));
    }

    private static void loadConfig(Class<?> targetClass, ConfigurationSection config) {
        for (Field field : targetClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            ConfigSection annotation = field.getAnnotation(ConfigSection.class);
            if (annotation == null) continue;
            field.setAccessible(true);

            Class<?> type = field.getType();
            try {
                if (type == int.class || type == Integer.class) {
                    field.set(null, config.getInt(annotation.value()));
                } else if (type == long.class || type == Long.class) {
                    field.set(null, config.getLong(annotation.value()));
                } else if (type == double.class || type == Double.class) {
                    field.set(null, config.getDouble(annotation.value()));
                } else if (type == boolean.class || type == Boolean.class) {
                    field.set(null, config.getBoolean(annotation.value()));
                } else if (type == String.class) {
                    field.set(null, config.getString(annotation.value()));
                } else if (Enum.class.isAssignableFrom(type)) {
                    field.set(null, EnumUtils.getEnumObj(type, config.getString(annotation.value())));
                }
//                System.out.println("Loaded config: " + annotation.value() + " = " + field.get(null) + " in " + targetClass.getName() + "#" + field.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static FileConfiguration createConfiguration(File file) throws Exception {
        String extension = StringUtils.getFilenameExtension(file.getName()).toLowerCase(Locale.ENGLISH);
        switch (extension) {
            case "json": return JsonConfiguration.loadConfiguration(file);
            case "toml": return TomlConfiguration.loadConfiguration(file);
            default: return YamlConfiguration.loadConfiguration(file);
        }
    }

    private static FileConfiguration createConfiguration(InputStream stream, String type) throws Exception {
        FileConfiguration config;
        switch (type) {
            case "json": config = new JsonConfiguration(); break;
            case "toml": config = new TomlConfiguration(); break;
            case "yml":
            case "yaml": config = new YamlConfiguration(); break;
            default:
                IllegalArgumentException exception = new IllegalArgumentException("Unknown configuration type: " + type);
                exception.setStackTrace(new StackTraceElement[0]);
                throw exception;
        }
        config.load(stream);
        return config;
    }

    private static FileConfiguration defaultConfiguration() throws Exception {
        return YamlConfiguration.loadConfiguration(() -> ConfigManager.class.getResourceAsStream("/default-config.yml"));
    }

    public static String saveAs(String type) throws Exception {
        FileConfiguration config;
        switch (type) {
            case "json":
                config = new JsonConfiguration();
                break;

            case "toml":
                config = new TomlConfiguration();
                break;

            default:
                config = new YamlConfiguration();
        }
        config.setDefaults(configuration);
        return config.saveToString();
    }
    public static void save(String type, String to) throws Exception {
        FileConfiguration config;
        switch (type) {
            case "json":
                config = new JsonConfiguration();
                break;

            case "toml":
                config = new TomlConfiguration();
                break;

            default:
                config = new YamlConfiguration();
        }
        config.setDefaults(configuration);
        config.save(to);
    }
}
