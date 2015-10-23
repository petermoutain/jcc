package com.onewaveinc.mrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * MIP 模块
 * 
 * @author gmice
 */
public class Module {
    
    public static final int MASK_EXCLUDE_DISABLED = 1;
    
    public static final int MASK_EXCLUDE_EXTENSION = 2;
    
    public static final int MASK_DEFAULT = MASK_EXCLUDE_DISABLED | MASK_EXCLUDE_EXTENSION;

    public final static String METADATA_FILE = "/META-INF/module.properties";

    private static final String MODULE_CLASSPATH = "module.classpath";

    private static final String MODULE_DESCRIPTION = "module.description";
    
    private static final String MODULE_EXTEND = "module.extend";

    private static final String MODULE_ID = "module.id";

    private static final String MODULE_LISTENER = "module.listener";

    private static final String MODULE_NAME = "module.name";

    private static final String MODULE_REQUIRE = "module.require";

    private static final String MODULE_REQUIRE_OPTIONAL = "module.require.optional";

    private static final String MODULE_STARTLEVEL = "module.startlevel";

    private static final String MODULE_VERSION = "module.version";

    private Map<String,Object> attributes = new HashMap<String,Object>();

    private List<String> classPath;

    private String description;

    private boolean enabled = true;
    
    private String extend;

    private List<Module> extensions;
    
    private String id;

    private int level;

    private ModuleEventListener listener;

    private Properties metadata;

    private ModuleContext moduleContext;

    private String name;

    private List<RequireEntry> require;

    private List<RequireEntry> requireOptional;

    private String rootDirectory;

    private int startLevel = Integer.MAX_VALUE;

    private ModuleState state;
    
    private Version version;
    
    /**
     * @param rootDirectory 模块根目录
     */
    public Module(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Module)) return false;
        Module anotherModule = (Module) obj;
        return id.equals(anotherModule.id) && version.equals(anotherModule.version);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public List<String> getClassPath() {
        return classPath;
    }

    public String getDescription() {
        return description;
    }

    public String getExtend() {
        return extend;
    }

    public List<Module> getExtensions() {
        return extensions;
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public ModuleEventListener getListener() {
        return listener;
    }

    public Properties getMetadata() {
        return metadata;
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    public ModuleFile getModuleFile(String modulePath) {
        if (extensions != null && !METADATA_FILE.equals(modulePath)) {
            for (int i = extensions.size() - 1; i >= 0; i--) {
                Module extension = extensions.get(i);
                ModuleFile moduleFile = extension.getModuleFile(modulePath);
                if (moduleFile.isFile()) {
                    return moduleFile.toExternal(this);
                }
            }
        }
        
        return new ModuleFile(this, modulePath);
    }

    public String getName() {
        return name;
    }

    public String getProperty(String key) {
        return metadata.getProperty(key);
    }

    public List<RequireEntry> getRequire() {
        return require;
    }

    public List<RequireEntry> getRequireOptional() {
        return requireOptional;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public int getStartLevel() {
        return startLevel;
    }

    public ModuleState getState() {
        return state;
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return (id + '/' + version).hashCode();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void setClassPath(List<String> classPath) {
        this.classPath = classPath;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    public void setExtensions(List<Module> extensions) {
        this.extensions = extensions;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setListener(ModuleEventListener listener) {
        this.listener = listener;
    }

    public void setMetadata(Properties metadata) {
        this.metadata = metadata;
    }

    public void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRequire(List<RequireEntry> require) {
        this.require = require;
    }

    public void setRequireOptional(List<RequireEntry> requireOptional) {
        this.requireOptional = requireOptional;
    }

    public void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }

    public void setState(ModuleState state) {
        this.state = state;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public String toString() {
        if (extensions == null) {
            return id + '/' + version;
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(id).append('/').append(version).append(" (");
            for (Iterator<Module> it = extensions.iterator(); it.hasNext(); ) {
                Module extension = it.next();
                builder.append(extension.id).append('/').append(extension.version);
                if (it.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(")");
            return builder.toString();
        }
    }

    /**
     * 根据指定模块根目录，载入模块元数据并构建模块实例
     * 
     * @param moduleDirectory 模块根目录
     * @return 模块实例
     * @throws ModuleException
     */
    public static Module load(File moduleDirectory) throws ModuleException {
        assert moduleDirectory.isDirectory();

        // 1. 载入模块元数据文件
        File metadataFile = new File(moduleDirectory.getAbsolutePath() + METADATA_FILE);
        Properties metadata = new Properties();
        try {
            metadata.load(new FileInputStream(metadataFile));
        } catch (FileNotFoundException e) {
            throw new ModuleException("不存在模块元数据文件： " + metadataFile.getAbsolutePath());
        } catch (IOException e) {
            throw new ModuleException("载入模块元数据文件" + metadataFile.getAbsolutePath() + " 时出错");
        }

        // 2. 创建模块实例
        Module module = new Module(moduleDirectory.getAbsolutePath());

        // 3. 解析模块 ID
        String id = metadata.getProperty(MODULE_ID);
        if (id == null || id.length() == 0) {
            throw new ModuleException(MODULE_ID + " 未定义");
        }
        module.setId(metadata.getProperty(MODULE_ID));

        // 4. 解析模块版本号
        String version = metadata.getProperty(MODULE_VERSION);
        if (version == null || version.length() == 0) {
            throw new ModuleException(MODULE_VERSION + " 未定义");
        }
        try {
            module.setVersion(Version.parse(metadata.getProperty(MODULE_VERSION)));
        } catch (VersionFormatException e) {
            throw new ModuleException("分析 " + MODULE_VERSION + " 时出错", e);
        }

        // 5. 解析模块依赖
        String require = metadata.getProperty(MODULE_REQUIRE);
        if (require != null && require.length() > 0) {
            List<RequireEntry> requireEntries = new ArrayList<RequireEntry>();
            try {
                for (String part : require.split(";")) {
                    requireEntries.add(RequireEntry.parse(part));
                }
            } catch (RequireEntryFormatException e) {
                throw new ModuleException("分析 " + MODULE_REQUIRE + " 时出错", e);
            }
            module.setRequire(requireEntries);
        }

        // 6. 解析模块可选依赖
        String requireOptional = metadata.getProperty(MODULE_REQUIRE_OPTIONAL);
        if (requireOptional != null && requireOptional.length() > 0) {
            List<RequireEntry> requireEntries = new ArrayList<RequireEntry>();
            try {
                for (String part : requireOptional.split(";")) {
                    requireEntries.add(RequireEntry.parse(part));
                }
            } catch (RequireEntryFormatException e) {
                throw new ModuleException("分析 " + MODULE_REQUIRE_OPTIONAL + " 时出错", e);
            }
            module.setRequireOptional(requireEntries);
        }

        // 7. 解析模块类路径
        String classPathString = metadata.getProperty(MODULE_CLASSPATH);
        List<String> classPath = new ArrayList<String>();
        if (classPathString != null && classPathString.length() > 0) {
            // 7.a 使用配置的类路径
            for (String path : classPathString.split(";")) {
                File f = new File(module.getRootDirectory() + path);
                classPath.add(f.getAbsolutePath());
            }
        } else {
            // 7.b 使用默认的类路径，即 /bin 以及 /lib/*.jar
            String root = module.getRootDirectory();
            classPath.add(new File(root, "bin").getAbsolutePath());
            File libDirectory = new File(root, "lib");
            if (libDirectory.isDirectory()) {
                for (File f : libDirectory.listFiles()) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        classPath.add(f.getAbsolutePath());
                    }
                }
            }
        }
        module.setClassPath(classPath);

        // 8. 解析模块事件监听器
        String listenerDef = metadata.getProperty(MODULE_LISTENER);
        if (listenerDef != null && listenerDef.length() > 0) {
            module.setListener(new LazyInitializedModuleEventListener(listenerDef));
        }

        // 9. 解析模块启动级别
        String startLevel = metadata.getProperty(MODULE_STARTLEVEL);
        if (startLevel != null && startLevel.length() > 0) {
            module.setStartLevel(Integer.parseInt(startLevel));
        }
        
        // 10. 解析模块扩展
        String extend = metadata.getProperty(MODULE_EXTEND);
        if (extend != null && extend.length() > 0) {
            module.setExtend(extend);
        }

        // 11. 解析其他模块元数据
        module.setName(metadata.getProperty(MODULE_NAME));
        module.setDescription(metadata.getProperty(MODULE_DESCRIPTION));
        module.setMetadata(metadata);

        return module;
    }

}
