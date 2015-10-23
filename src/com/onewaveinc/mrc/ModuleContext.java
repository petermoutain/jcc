package com.onewaveinc.mrc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;

import sun.misc.BASE64Decoder;

import com.onewaveinc.mrc.LicenseException.Code;
import com.onewaveinc.mrc.console.Console;
import com.onewaveinc.mrc.console.DefaultConsoleService;

/**
 * MRC。MIP 核心。负责模块安装及生命周期的管理
 * 
 * @author gmice
 * 
 */
public class ModuleContext {

    public static interface Attributes {
        String CONSOLE_SERVICE = "CONSOLE_SERVICE";

        String HTTP_SERVICE = "HTTP_SERVICE";
        
        String HTTP_SESSION_ATTRIBUTE_LISTENER = "HTTP_SESSION_ATTRIBUTE_LISTENER";

        String HTTP_SESSION_LISTENER = "HTTP_SESSION_LISTENER";

        String HTTP_STATIC_CONTENT = "HTTP_STATIC_CONTENT";

        String SERVLET_CONTEXT = "MRC_SERVLET_CONTEXT";
        
        String SERVLET_CONTEXT_ATTRIBUTE_LISTENER = "SERVLET_CONTEXT_ATTRIBUTE_LISTENER";
        
        String SERVLET_REQUEST_ATTRIBUTE_LISTENER = "SERVLET_REQUEST_ATTRIBUTE_LISTENER";
        
        String SERVLET_REQUEST_LISTENER = "SERVLET_REQUEST_LISTENER";
    }

    private static interface LicenseAttributes {
        String LICENSE = "License";
    }

    private static interface ManifestAttributes {
        String EXPIRED_TIME = "Expired-Time";
        
        String GIT_COMMIT_ID = "Git-Commit-Id";
        
        String IMPLEMENTATION_VERSION = "Implementation-Version";

        String PUBLIC_KEY = "Public-Key";

        String SIGNATURE = "Signature";
    }

    public static final String MODULE_FILE_SUFFIX = ".mod";

    public static final String MODULE_LINK_SUFFIX = ".link";
    
    public static final String PROPERTIES_FILE = "mrc.properties";

    public static final String PROPERTY_MODULES_DIR = "modules.dir";

    public static final String PROPERTY_MODULES_DIR_DEFAULT_VALUE = "../modules";

    public static final String PROPERTY_MODULES_DISABLED = "modules.disabled";

    public static final String PROPERTY_MODULES_STARTLEVEL = "modules.startlevel";
    
    public static final String PROPERTY_MODULES_TEST = "modules.test";
    
    public static final String PROPERTY_MRC_CONSOLE_DISABLED = "mrc.console.disabled";

    public static final String PROPERTY_MRC_FAILURE = "mrc.failure";

    public static final String PROPERTY_MRC_PHASE = "mrc.phase";

    public static final String PROPERTY_MRC_READONLY = "mrc.readonly";

    public static final String PROPERTY_MRC_STARTLEVEL_SAFE = "mrc.startlevel.safe";

    public static final String PROPERTY_MRC_STARTLEVEL_SAFE_DEFAULT_VALUE = "10";
    
    public static final String SERVLET_CONTEXT_ATTRIBUTE = "OW_MODULE_CONTEXT";

    private static final String LICENSE_FILE = "license.key";

    private static final String LICENSE_KEY_ALGORITHM = "RSA";
    
    private static final Logger logger = Logger.getLogger(ModuleContext.class.getName());

    private static final byte MAGIC_CODE = 0x11;

    private static final String MODULE_MANIFEST_FILE = "/META-INF/MANIFEST.MF";
    
    /**
     * 模块间共享的属性
     */
    private Map<String,Object> attributes = new HashMap<String,Object>();

    /**
     * 全局类装载器
     */
    private ModuleContextClassLoader classLoader;

    private Manifest manifest;

    /**
     * 模块列表
     */
    private List<Module> modules = new ArrayList<Module>();

    /**
     * 模块安装目录列表
     */
    private List<File> modulesDirectory;
    
    private volatile SoftReference<Map<URL,Module>> modulesURLCache = new SoftReference<Map<URL,Module>>(null);

    /**
     * MRC 配置
     */
    private Properties properties = new Properties();
    
    /**
     * MRC 配置是否被修改
     */
    private boolean propertiesModified = false;

    /**
     * 许可证解密用公钥
     */
    private PublicKey publicKey;

    /**
     * 注册资源
     */
    private Map<String,URL> resourceURLs = new HashMap<String,URL>();

    /**
     * MRC 根目录
     */
    private String rootDirectory;

    /**
     * MRC 是否运行中
     */
    private boolean running = false;
    
    /**
     * 最大启动级别
     */
    private int startLevel = Integer.MAX_VALUE;
    
    /**
     * 构建 MRC 实例
     * 
     * @param rootDirectory MRC 启动根目录
     */
    public ModuleContext(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    /**
     * 构建 MRC 实例
     * 
     * @param rootDirectory MRC 启动根目录
     * @param defaultProperties 默认 MRC 配置
     */
    public ModuleContext(String rootDirectory, Properties defaultProperties) {
        this.rootDirectory = rootDirectory;
        this.properties = defaultProperties;
    }

    /**
     * 获取模块间共享的属性值
     * 
     * @param key 属性名
     * @return 属性值
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 获取全局类装载器
     * 
     * @return
     */
    public ModuleContextClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * 根据模块 ID 获取一个已启用的模块
     * 
     * @param id 模块 ID
     * @return 模块实例
     */
    public Module getModule(String id) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                if (module.getId().equals(id)) return module;
            }
        }
        return null;
    }

    /**
     * 根据模块 ID 和版本号，获取一个安装的模块
     * 
     * @param id 模块 ID
     * @param version 模块版本号
     * @return 模块实例
     */
    public Module getModule(String id, Version version) {
        for (Module module : modules) {
            if (module.getId().equals(id) && module.getVersion().equals(version)) return module;
        }
        return null;
    }

    /**
     * 根据模块 ID 获取该模块的扩展模块列表
     * 
     * @param id 模块 ID
     * @return 扩展模块实例
     */
    public List<Module> getModuleExtensions(String id) {
        for (Module module : modules) {
            if (module.isEnabled() && module.getExtend() != null && module.getExtend().equals(id)) {
                List<Module> extensions = getModuleExtensions(module.getId());
                if (extensions != null) {
                    extensions.add(module);
                } else {
                    extensions = new ArrayList<Module>();
                    extensions.add(module);
                }
                return extensions;
            }
        }
        return null;
    }
    
    /**
     * 获取指定路径的资源所在的模块
     * 
     * @param path 资源文件路径
     * @return 资源文件所在的模块
     */
    public Module getModuleOfResource(String path) {
        return getModuleOfResource(getResource(path));
    }
    
    /**
     * 获取指定URL的资源所在的模块
     * 
     * @param url 资源文件URL
     * @return 资源文件所在的模块
     */
    public Module getModuleOfResource(URL url) {
        if (url == null) return null;
        try {
            if ("jar".equals(url.getProtocol())) {
                try {
                    url = ((JarURLConnection) url.openConnection()).getJarFileURL();
                } catch (IOException ignore) {
                    return null;
                }
            }
            
            Map<URL,Module> modulesURL = modulesURLCache.get();
            if (modulesURL == null) {
                synchronized (this) {
                    modulesURL = modulesURLCache.get();
                    if (modulesURL == null) {
                        final Map<URL,Module> _modulesURL = modulesURL = new HashMap<URL,Module>();
                        visitModules(new ModuleVisitor() {
                            public void visit(Module module) throws Exception {
                                File file = new File(module.getRootDirectory());
                                _modulesURL.put(file.toURI().toURL(), module);
                            }
                        });
                        modulesURLCache = new SoftReference<Map<URL,Module>>(_modulesURL);
                    }
                }
            }
            
            for (;;) {
                Module module = modulesURL.get(url);
                if (module != null) return module;
                
                File file = new File(url.toURI()).getParentFile();
                if (file == null) break;
                
                url = file.toURI().toURL();
            }
        } catch (IOException ignore) {
            // EMPTY
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    /**
     * 获得所有安装的模块
     * 
     * @return 模块列表
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * 获取 MRC 配置项的值
     * 
     * @param key 配置项名
     * @return 配置项的值
     */
    public String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) value = properties.getProperty(key);
        return value;
    }

    /**
     * 获取 MRC 配置项的值。当配置项不存在时，返回指定的默认值
     * 
     * @param key 配置项名
     * @param defaultValue 默认值
     * @return 配置项的值
     */
    public String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null) value = properties.getProperty(key, defaultValue);
        return value;
    }

    /**
     * 根据指定路径，获取注册的资源。注册的资源包括类装载路径中的文件以及通过 registerResource 方法额外注册的文件
     * 
     * @param path 路径，/path/to/resource
     * @return 资源的 URL。无资源时，返回 null
     */
    public URL getResource(String path) {
        if (path == null) return null;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return classLoader.getResource(path);
    }

    /**
     * 根据指定路径，获取注册的资源的输入流。注册的资源包括类装载路径中的文件以及通过 registerResource 方法额外注册的文件
     * 
     * @param path 路径，/path/to/resource
     * @return 资源的输入流。无资源时，返回 null
     */
    public InputStream getResourceAsStream(String path) {
        URL url = getResource(path);
        if (url != null) {
            try {
                return url.openStream();
            } catch (IOException ignore) {}
        }
        return null;
    }

    /**
     * 获取 MRC 根目录
     * 
     * @return MRC 根目录
     */
    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getVersion() {
        java.util.jar.Attributes attributes = manifest.getMainAttributes();
        return attributes.getValue(ManifestAttributes.IMPLEMENTATION_VERSION) + 
            " (tag " + attributes.getValue(ManifestAttributes.GIT_COMMIT_ID) + ")";
    }

    /**
     * 判断 MRC 是否已正常启动
     * 
     * @return 当且仅当 MRC 已正常启动，返回 true
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 注册资源
     * 
     * @param path 资源路径，/path/to/resource
     * @param file 资源文件
     */
    public void registerResource(String path, File file) {
        try {
            registerResource(path, file.toURI().toURL());
        } catch (MalformedURLException ignore) {}
    }

    /**
     * 注册资源
     * 
     * @param path 资源路径，/path/to/resource
     * @param url 资源 URL
     */
    public void registerResource(String path, URL url) {
        if (path.startsWith("/")) path = path.substring(1);
        URL original = resourceURLs.put(path, url);
        if (original != null) {
            logger.warning("重复注册资源，路径：" + path + ", " + original + " => " + url);
        }
    }
    
    /**
     * 重新读取许可证文件，更新许可证信息
     */
    @SuppressWarnings("unchecked")
    public void reloadLicense() throws LicenseException {
        if (!License.enabled()) {
            throw new IllegalStateException("许可证未启用，无法重新读取");
        }
        
        // 读取许可证文件
        File file = new File(rootDirectory, LICENSE_FILE);
        
        byte[] encryptedLicenseData = loadLicense(file, LicenseAttributes.LICENSE);
        byte[] licenseData = null;
        try {
            licenseData = decryptLicenseData(encryptedLicenseData);
        } catch (Throwable e) {
            throw new LicenseException(Code.BAD_LICENSE_DATA);
        }

        // 验证许可证功能模块
        validateLicenseModule(licenseData);

        // 保存License数据以被模块使用
        License.putAll((Map<String,byte[]>) License.deserialize(licenseData));
    }
    
    /**
     * 删除 MRC 配置项
     * 
     * @param key 配置项名
     */
    public void removeProperty(String key) {
        properties.remove(key);
        propertiesModified = true;
    }

    /**
     * 启动 MRC。如果 MRC 已经启动，则不会有任何效果
     */
    public synchronized void run() {
        if (running) return;

        ClassLoader original = Thread.currentThread().getContextClassLoader();

        try {
            loadManifest();
            
            logger.info("MRC 版本：" + getVersion());
            logger.info("MRC 启动开始，根目录：" + rootDirectory);

            loadProperties();

            if (modulesDirectory == null || modulesDirectory.isEmpty()) {
                throw new IOException("MIP 模块安装目录不存在");
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append("MIP 模块安装目录：");
                for (Iterator<File> it = modulesDirectory.iterator(); it.hasNext();) {
                    builder.append(it.next().getAbsolutePath());
                    if (it.hasNext()) {
                        builder.append("; ");
                    }
                }
                logger.info(builder.toString());
            }

            unzipModuleFiles();

            installModules();
            
            installTestModules();

            loadLicense();

            disableModules();

            checkCompatibility();

            // 设置模块的扩展模块
            // TODO: 采用效率高的算法
            visitModules(new ModuleVisitor() {
                public void visit(Module module) throws Exception {
                    module.setExtensions(getModuleExtensions(module.getId()));
                }
            }, Module.MASK_EXCLUDE_DISABLED);

            logger.info("按启动顺序和模块ID排序模块");
            Collections.sort(modules, new Comparator<Module>() {
                public int compare(Module o1, Module o2) {
                    int c = o1.getStartLevel() - o2.getStartLevel();
                    if (c == 0) {
                        c = o1.getId().compareTo(o2.getId());
                    }
                    return c;
                }
            });

            checkStartLevel();

            logger.info("创建类装载器");
            final List<URL> urls = new ArrayList<URL>();
            visitModules(new ModuleVisitor() {
                public void visit(Module module) throws Exception {
                    if (module.getStartLevel() > startLevel) return;

                    List<URL> moduleUrls = new ArrayList<URL>();

                    // 如果存在扩展模块，则优先使用扩展模块的类路径，以达到覆盖的效果
                    if (module.getExtensions() != null) {
                        for (Module extension : module.getExtensions()) {
                            for (String path : extension.getClassPath()) {
                                moduleUrls.add(new File(path).toURI().toURL());
                            }
                        }
                    }

                    for (String path : module.getClassPath()) {
                        moduleUrls.add(new File(path).toURI().toURL());
                    }

                    // 启动顺序越高的模块的路径在越前面，这么做允许一个模块通过覆盖的方法修改其依赖模块的某些类，但不鼓励这种行为
                    // FIXME: 根据依赖关系而不是启动顺序决定类装载路径顺序
                    urls.addAll(0, moduleUrls);
                }
            });
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ModuleContextClassLoader mrcClassLoader;
            if (contextClassLoader instanceof ModuleContextClassLoader) {
                mrcClassLoader = (ModuleContextClassLoader) contextClassLoader;
                mrcClassLoader.addURLs(urls);
            } else {
                mrcClassLoader = new ModuleContextClassLoader(urls.toArray(new URL[0]), contextClassLoader);
                mrcClassLoader.setModuleContext(this);
            }
            mrcClassLoader.setResourceURLs(resourceURLs);
            classLoader = mrcClassLoader;

            Thread.currentThread().setContextClassLoader(classLoader);

            String phase = getProperty(PROPERTY_MRC_PHASE);

            if ("install".equalsIgnoreCase(phase)) {
                logger.info(PROPERTY_MRC_PHASE + " 设置为 install，MRC 启动中止");
                return;
            }

            prepare();

            if ("prepare".equalsIgnoreCase(phase)) {
                logger.info(PROPERTY_MRC_PHASE + " 设置为 prepare，MRC 启动中止");
                return;
            }

            resolve();

            if ("resolve".equalsIgnoreCase(phase)) {
                logger.info(PROPERTY_MRC_PHASE + " 设置为 resolve，MRC 启动中止");
                return;
            }

            start();

            if (!License.enabled()) {
                printDevelopVersion();
            }

            logger.info("MRC 启动完成");
            running = true;

            storeProperties();
        } catch (Throwable e) {
            if (e instanceof LicenseException) {
                // 当由于版权问题启动失败时，隐藏调用栈信息
                logger.log(Level.SEVERE, e.getMessage());
            } else {
                logger.log(Level.SEVERE, "MRC 启动失败", e);
            }
            // 设置启动失败状态
            setProperty(PROPERTY_MRC_FAILURE, "true");
            storeProperties();
            printFailMessage();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }

        // 启动控制台服务
        if (!Boolean.parseBoolean(this.getProperty(PROPERTY_MRC_CONSOLE_DISABLED, "false"))) {
            if (getAttribute(Attributes.CONSOLE_SERVICE) == null) {
                setAttribute(Attributes.CONSOLE_SERVICE, new DefaultConsoleService(this));
            }
            Console.start(this);
        }
    }

    /**
     * 设置模块间共享属性值
     * 
     * @param key 属性名
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 设置 MRC 配置项。必须在 MRC 启动前设置才有效果
     * 
     * @param key 配置项名
     * @param value 配置项值
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
        propertiesModified = true;
    }

    /**
     * 停止 MRC。如果 MRC 未运行，则不会有任何效果
     */
    public synchronized void stop() {
        if (!running) return;

        try {
            throw new ModuleContextStop();
        } catch (ModuleContextStop e) {
            logger.log(Level.INFO, "MRC 关闭", e);
        }

        try {
            visitModules(new ModuleVisitor() {
                public void visit(Module module) throws Exception {
                    logger.info("停止模块：" + module);
                    ModuleEventListener listener = module.getListener();
                    if (listener != null) {
                        ModuleEvent event = new ModuleEvent(ModuleEvent.STOP, module, ModuleContext.this);
                        listener.listen(event);
                    }
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "MRC 停止失败", e);
        }
        
        if (!Boolean.parseBoolean(this.getProperty(PROPERTY_MRC_CONSOLE_DISABLED, "false"))) {
            Console.stop();
        }

        running = false;
    }

    /**
     * 保存 MRC 配置文件
     */
    public void storeProperties() {
        // 配置未修改，不保存
        if (!propertiesModified) return;

        // 配置为只读，不保存
        boolean readonly = Boolean.valueOf(getProperty(PROPERTY_MRC_READONLY));
        if (readonly) {
            logger.info("未保存 MRC 配置文件，因为 " + PROPERTY_MRC_READONLY + " 设置为 true");
            return;
        }

        logger.info("保存 MRC 配置文件");
        File file = new File(rootDirectory, PROPERTIES_FILE);
        try {
            properties.store(new FileOutputStream(file), "MRC configuration");
        } catch (IOException e) {
            logger.log(Level.WARNING, "保存 MRC 配置文件时出错", e);
        }
    }

    /**
     * 遍历所有已启用模块下的指定文件。对每一个文件，回调传入的访问者
     * 
     * @param modulePath 文件相对于模块的路径
     * @param visitor 处理回调的访问者实例
     * @throws Exception
     */
    public void visitModuleFiles(String modulePath, ModuleFileVisitor visitor) throws Exception {
        for (Module module : modules) {
            if (module.isEnabled() && module.getExtend() == null) {
                ModuleFile file = module.getModuleFile(modulePath);
                if (file.exists()) visitor.visit(file);
            }
        }
    }

    /**
     * 遍历所有已启用的基本模块。对每一个模块，回调传入的访问者
     * 
     * @param visitor 处理回调的访问者实例
     * @throws Exception
     */
    public void visitModules(ModuleVisitor visitor) throws Exception {
        visitModules(visitor, Module.MASK_DEFAULT);
    }

    /**
     * 遍历所有模块。对每一个模块，如果不被指定的掩码过滤，则回调传入的访问者
     * 
     * @param visitor 处理回调的访问者实例
     * @param mask 掩码
     * @throws Exception
     */
    public void visitModules(ModuleVisitor visitor, int mask) throws Exception {
        for (Module module : modules) {
            if ((mask & Module.MASK_EXCLUDE_DISABLED) != 0 && !module.isEnabled()) continue;
            if ((mask & Module.MASK_EXCLUDE_EXTENSION) != 0 && module.getExtend() != null) continue;
            visitor.visit(module);
        }
    }

    private void checkCompatibility() throws ModuleException, UnsatisfiedDependencyException {
        logger.info("模块相容性检查");
        new StrictCompatibilityChecker(this).execute();
    }

    private void checkStartLevel() {
        if ("true".equalsIgnoreCase(getProperty(PROPERTY_MRC_FAILURE))) {
            /** 暂时不支持安全模式，等模块提供相应支持 */
            /*
            System.out.println("MRC 上次启动失败，是否进入安全模式？");
            System.out.print("(y/n) n: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = null;
            try {
                line = in.readLine();
            } catch (IOException ignore) {}
            if (line != null && "y".equalsIgnoreCase(line)) {
                startLevel = Integer.parseInt(getProperty(PROPERTY_MRC_STARTLEVEL_SAFE, PROPERTY_MRC_STARTLEVEL_SAFE_DEFAULT_VALUE));
            }
            */

            // 清除启动失败状态
            removeProperty(PROPERTY_MRC_FAILURE);
        }

        String value = getProperty(PROPERTY_MODULES_STARTLEVEL);
        if (value != null && value.length() != 0) {
            startLevel = Math.min(startLevel, Integer.parseInt(value));
        }

        if (startLevel == Integer.MAX_VALUE) {
            logger.info("MRC 启动级别：全部启动");
        } else {
            logger.info("MRC 启动级别：仅启动 startlevel <= " + startLevel + " 的模块");
        }

    }

    /**
     * 解密许可证信息
     * 
     * @param source 加密的许可证信息
     * @param key 密钥
     * @return 已解密的许可证信息
     * @throws Exception
     */
    private byte[] decryptLicenseData(byte[] source) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Cipher cipher = Cipher.getInstance(LICENSE_KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        int blockSize = 128;
        int index = 0;
        while (source.length > index) {
            if (source.length >= index + blockSize) {
                out.write(cipher.doFinal(source, index, blockSize));
            } else {
                out.write(cipher.doFinal(source, index, source.length % blockSize));
            }
            index += blockSize;
        }
        return out.toByteArray();
    }

    /**
     * 删除目录（递归删除目录下所有目录和文件）或文件
     * 
     * @param file 目录或文件
     * @return 当且仅当删除成功时，返回 true
     */
    private boolean deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!deleteDirectory(f)) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    /**
     * 停用通过 modules.disabled 属性配置的部分模块
     * 
     * @throws ModuleException
     */
    private void disableModules() throws ModuleException {
        String disableModules;
        if (License.ENABLED) {
            disableModules = (String) License.deserialize(License.get(ModuleContext.PROPERTY_MODULES_DISABLED));
        } else {
            disableModules = getProperty(ModuleContext.PROPERTY_MODULES_DISABLED);
        }
        if (disableModules == null || disableModules.length() == 0) return;
        for (String disableModule : disableModules.split(";\\s*")) {
            String[] parts = disableModule.split("/");

            if (parts.length == 0 || parts.length > 2) {
                logger.warning("停用模块配置格式错误：" + disableModule);
                continue;
            }

            final String id = parts[0];
            final VersionRange versionRange = (parts.length == 1) ? VersionRange.ALL : VersionRange.parse(parts[1]);

            try {
                visitModules(new ModuleVisitor() {
                    public void visit(Module module) throws Exception {
                        if (id.equals(module.getId()) && versionRange.contains(module.getVersion())) {
                            logger.info("停用模块：" + module);
                            module.setEnabled(false);
                        }
                    }
                });
            } catch (Exception e) {
                throw new ModuleException("停用模块时出错", e);
            }
        }
    }

    private void install(Module module) throws ModuleException {
        assert module != null && module.getModuleContext() == null;
        assert module.getState() == null;
        module.setModuleContext(this);
        module.setState(ModuleState.INSTALLED);
        modules.add(module);
    }

    /**
     * 安装 MIP 模块安装目录下的所有 MIP 模块
     * 
     * @param modulesDirectory MIP 模块安装目录
     */
    private void installModules() {
        final Set<String> directoriesToIgnore = new HashSet<String>(Arrays.asList("CVS", ".git", ".svn"));
        
        int level = 0; // 模块级别，模块属于越靠前的模块安装目录时，级别越低
        for (File directory : modulesDirectory) {
            for (File moduleFile : directory.listFiles()) {
                if (moduleFile.isFile()) {
                    if (moduleFile.getName().endsWith(ModuleContext.MODULE_LINK_SUFFIX)) {
                        // 处理模块链接文件
                        String link;
                        try {
                            // 读取模块链接文件的第一行，作为链接的路径
                            BufferedReader in = new BufferedReader(new FileReader(moduleFile));
                            link = in.readLine();
                            in.close();
                        } catch (FileNotFoundException ignore) {
                            continue;
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "读取模块链接文件 " + moduleFile.getAbsolutePath() + " 时出错", e);
                            continue;
                        }

                        if (link.matches("(/|\\\\\\\\|\\w:).*")) {
                            // 链接路径为绝对路径
                            moduleFile = new File(link);
                        } else {
                            // 链接路径为相对路径，相对于链接文件所在目录
                            moduleFile = new File(directory, link);
                        }

                        try {
                            moduleFile = moduleFile.getCanonicalFile();
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "访问目录 " + moduleFile.getAbsolutePath() + " 时出错", e);
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    if (directoriesToIgnore.contains(moduleFile.getName())) {
                        continue;
                    }
                }
                Module module = null;
                try {
                    module = Module.load(moduleFile);
                } catch (ModuleException e) {
                    logger.log(Level.WARNING, "分析目录 " + moduleFile.getAbsolutePath() + " 时出错", e);
                    continue;
                }
                module.setLevel(level);
                try {
                    install(module);
                } catch (ModuleException e) {
                    logger.log(Level.WARNING, "安装模块 " + module + " 时出错，模块目录：" + moduleFile.getAbsolutePath(), e);
                    continue;
                }
                logger.info("已安装模块：" + module + " 模块目录：" + moduleFile.getAbsolutePath());
            }

            level++;
        }
    }

    /**
     * 安装模块的测试扩展
     * 
     * @throws ModuleException
     */
    private void installTestModules() throws ModuleException {
        String testModules = getProperty(ModuleContext.PROPERTY_MODULES_TEST);
        if (testModules == null || testModules.length() == 0) return;
        for (String testModule : testModules.split(";\\s*")) {
            String[] parts = testModule.split("/");

            if (parts.length == 0 || parts.length > 2) {
                logger.warning("测试模块配置格式错误：" + testModules);
                continue;
            }

            final String id = parts[0];
            final Version version = (parts.length > 1 ? Version.parse(parts[1]) : null);

            final List<Module> testModulesToInstall = new ArrayList<Module>();
            try {
                visitModules(new ModuleVisitor() {
                    public void visit(Module module) throws Exception {
                        if (id.equals(module.getId()) && (version == null || version.equals(module.getVersion()))) {
                            logger.info("测试模块：" + module);
                            
                            String rootDirectory = module.getRootDirectory() + "/test";
                            if (new File(rootDirectory).isDirectory()) {
                                Module testModule = new Module(rootDirectory);
                                testModule.setId(module.getId() + "#test");
                                testModule.setVersion(module.getVersion());
                                testModule.setExtend(module.getId());
                                testModule.setClassPath(Arrays.asList(rootDirectory + "/bin"));
                                
                                testModulesToInstall.add(testModule);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                throw new ModuleException("安装测试模块时出错", e);
            }
            
            if (!testModulesToInstall.isEmpty()) {
                for (Module testModuleToInstall : testModulesToInstall) {
                    install(testModuleToInstall);
                }
            }
        }
    }
    
    /**
     * 1. 检查许可证文件是否存在，并读取加密的许可证信息<br>
     * 2. 解密许可证信息并解析，确认许可证功能模块在系统中存在，且并未被纂改<br>
     * 
     * @throws LicenseException
     */
    @SuppressWarnings("unchecked")
    private void loadLicense() throws LicenseException {
        Long expiredTime = null;

        /** 从manifest中读取公钥和过期时间信息 */
        try {
            java.util.jar.Attributes attributes = manifest.getMainAttributes();

            // 读取简单加密的公钥数据，这是为了防止公钥被替换
            byte[] publicKeyData = new BASE64Decoder().decodeBuffer(attributes.getValue(ManifestAttributes.PUBLIC_KEY));

            // 解密公钥数据
            for (int i = 0; i < publicKeyData.length; i++) {
                publicKeyData[i] ^= MAGIC_CODE;
            }

            // 构建公钥
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyData);
            KeyFactory keyFactory = KeyFactory.getInstance(LICENSE_KEY_ALGORITHM);
            publicKey = keyFactory.generatePublic(keySpec);

            // 获取并解密过期时间信息
            try {
                byte[] encryptedExpiredTimeData = new BASE64Decoder().decodeBuffer(attributes.getValue(ManifestAttributes.EXPIRED_TIME));
                byte[] expiredTimeData = decryptLicenseData(encryptedExpiredTimeData);
                expiredTime = Long.valueOf(new String(expiredTimeData));
            } catch (Exception e) {
                throw new LicenseException(Code.MISSING_EXPIRED_TIME);
            }
        } catch (LicenseException e) {
            throw e;
        } catch (Throwable e) {
            throw new LicenseException("MRC 未授权：" + e.getMessage());
        }

        // 读取许可证文件
        File file = new File(rootDirectory, LICENSE_FILE);
        if (!file.exists()) {
            // 许可证文件不存在，检查过期时间决定是否启用试用模式
            if (System.currentTimeMillis() > expiredTime) {
                throw new LicenseException("MRC 过期。可以重新执行 mip init 来更新 MRC");
            } else {
                License.setEnable(false);
            }
        } else {
            byte[] encryptedLicenseData = loadLicense(file, LicenseAttributes.LICENSE);
            byte[] licenseData = null;
            try {
                licenseData = decryptLicenseData(encryptedLicenseData);
            } catch (Throwable e) {
                throw new LicenseException(Code.BAD_LICENSE_DATA);
            }

            // 验证许可证功能模块
            validateLicenseModule(licenseData);

            // 保存License数据以被模块使用
            License.putAll((Map<String,byte[]>) License.deserialize(licenseData));
        }
    }

    /**
     * 
     * @param file manifest 文件
     * @param attribute manifest文件中的属性.
     * @return
     * @throws LicenseException
     */
    private byte[] loadLicense(File file, String attribute) throws LicenseException {
        try {
            Manifest manifest = new Manifest(new FileInputStream(file));
            java.util.jar.Attributes attributes = manifest.getMainAttributes();

            // 读取 RSA 加密的许可证信息
            String value = attributes.getValue(attribute);
            if (value == null) {
                throw new LicenseException(Code.MISSING_MANIFEST_ATTRIBUTE);
            }
            return new BASE64Decoder().decodeBuffer(value);
        } catch (LicenseException e) {
            throw e;
        } catch (IOException e) {
            throw new LicenseException("MRC 未授权，IO 异常：" + e.getMessage());
        } catch (Throwable e) {
            throw new LicenseException("MRC 未授权：" + e.getMessage());
        }
    }
    
    private void loadManifest() throws IOException, LicenseException {
        // 确保ModuleContext类是从mrc.jar文件中加载的
        URL url = ModuleContext.class.getResource("/" + this.getClass().getName().replace('.', '/') + ".class");
        if (url == null || !"jar".equals(url.getProtocol())) {
            throw new LicenseException(Code.BAD_CLASS);
        }

        manifest = ((JarURLConnection) url.openConnection()).getManifest();
    }

    /**
     * 载入 MRC 配置文件
     */
    private void loadProperties() throws ModuleException {
        File file = new File(rootDirectory, PROPERTIES_FILE);
        logger.info("载入 MRC 配置文件：" + file.getAbsolutePath());
        if (file.exists()) {
            Properties defaultProperties = properties;
            try {
                properties.load(new FileInputStream(file));
                for (Object key : defaultProperties.keySet()) {
                    if (!properties.containsKey(key)) {
                        properties.setProperty((String) key, defaultProperties.getProperty((String) key));
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "读取 MRC 配置文件时出错，使用默认配置", e);
                properties = defaultProperties;
            }
        } else {
            logger.warning("MRC 配置文件不存在，使用默认配置");
        }

        modulesDirectory = new ArrayList<File>();
        for (String path : getProperty(ModuleContext.PROPERTY_MODULES_DIR, ModuleContext.PROPERTY_MODULES_DIR_DEFAULT_VALUE).split(";")) {
            File directory = new File(rootDirectory, path.trim());
            if (directory.isDirectory()) {
                try {
                    // 将模块安装目录转换成规范形式，有助于日志分析
                    modulesDirectory.add(directory.getCanonicalFile());
                } catch (IOException ignore) {}
            }
        }
    }

    private void prepare() throws Exception {
        visitModules(new ModuleVisitor() {
            public void visit(Module module) throws Exception {
                if (module.getStartLevel() > startLevel) return;
                logger.info("准备模块：" + module);
                ModuleEventListener listener = module.getListener();
                if (listener != null) {
                    ModuleEvent event = new ModuleEvent(ModuleEvent.PREPARE, module, ModuleContext.this);
                    listener.listen(event);
                }
            }
        });
    }

    private void printDevelopVersion() {
        final String CRLF = System.getProperty("line.separator");
        StringBuilder s = new StringBuilder();
        s.append("◇◆◇◆◇◆◇◆◇◆◇◆◇").append(CRLF);
        s.append("◆  注意：MRC 是试用版  ◆").append(CRLF);
        s.append("◇◆◇◆◇◆◇◆◇◆◇◆◇").append(CRLF);
        System.err.print(s);
    }

    private void printFailMessage() {
        final String CRLF = System.getProperty("line.separator");
        StringBuilder s = new StringBuilder();
        s.append("◇◆◇◆◇◆◇◆◇◆◇◆◇").append(CRLF);
        s.append("◆  注意：MRC 启动失败  ◆").append(CRLF);
        s.append("◇◆◇◆◇◆◇◆◇◆◇◆◇").append(CRLF);
        System.err.print(s);
    }

    private void resolve() throws Exception {
        visitModules(new ModuleVisitor() {
            public void visit(Module module) throws Exception {
                if (module.getStartLevel() > startLevel) return;
                logger.info("解析模块：" + module);
                ModuleEventListener listener = module.getListener();
                if (listener != null) {
                    ModuleEvent event = new ModuleEvent(ModuleEvent.RESOLVE, module, ModuleContext.this);
                    listener.listen(event);
                }
            }
        });
    }

    private void start() throws Exception {
        visitModules(new ModuleVisitor() {
            public void visit(Module module) throws Exception {
                if (module.getStartLevel() > startLevel) return;
                logger.info("启动模块：" + module);
                ModuleEventListener listener = module.getListener();
                if (listener != null) {
                    ModuleEvent event = new ModuleEvent(ModuleEvent.START, module, ModuleContext.this);
                    listener.listen(event);
                }
            }
        });
    }

    /**
     * 解压安装包文件
     * 
     * @param moduleFile 安装包文件
     * @param moduleDirectory 目标目录
     * @throws IOException
     */
    private void unzip(File moduleFile, File moduleDirectory) throws IOException {
        ZipInputStream in = new ZipInputStream(new FileInputStream(moduleFile));
        try {
            byte buf[] = new byte[8192];
            ZipEntry zipEntry = null;
            while ((zipEntry = in.getNextEntry()) != null) {
                File file = new File(moduleDirectory, zipEntry.getName());
                file.getParentFile().mkdirs();
                if (!zipEntry.isDirectory()) {
                    FileOutputStream out = new FileOutputStream(file);
                    try {
                        int i;
                        while ((i = in.read(buf)) != -1) {
                            out.write(buf, 0, i);
                        }
                    } finally {
                        out.close();
                    }
                }
                in.closeEntry();
            }
        } finally {
            in.close();
        }
    }

    /**
     * 遍历 MIP 模块安装目录，解压所有的安装包文件
     */
    private void unzipModuleFiles() {
        for (File directory : modulesDirectory) {
            for (File moduleFile : directory.listFiles()) {
                if (moduleFile.isFile() && moduleFile.getName().endsWith(ModuleContext.MODULE_FILE_SUFFIX)) {
                    String moduleFileName = moduleFile.getName();

                    // 获取安装包对应的安装目录
                    File moduleDirectory = new File(directory, moduleFileName.substring(0, moduleFileName.length() - ModuleContext.MODULE_FILE_SUFFIX.length()));

                    // 通过比较修改时间戳，若安装包已解压且没有更新，则不需要再次解压
                    if (moduleDirectory.isDirectory() && moduleDirectory.lastModified() >= moduleFile.lastModified()) {
                        continue;
                    }

                    // 解压
                    try {
                        unzip(moduleFile, moduleDirectory);
                        logger.info(String.format("解压文件 %s 至目录 %s", moduleFile.getAbsolutePath(), moduleDirectory.getAbsolutePath()));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, String.format("解压文件 %s 至目录 %s 时出错", moduleFile.getAbsolutePath(), moduleDirectory.getAbsolutePath()), e);
                        // 删除目标目录
                        if (!deleteDirectory(moduleDirectory)) {
                            logger.log(Level.SEVERE, "删除目录 " + moduleDirectory.getAbsolutePath() + " 失败");
                        }
                    }
                }
            }
        }
    }

    private void validateLicenseModule(byte[] licenseData) throws LicenseException {
        Module module = getModule("mip.license"); // FIXME
        if (module == null) {
            throw new LicenseException(Code.MISSING_LICENSE_MODULE);
        }
        
        ModuleFile manifestFile = module.getModuleFile(MODULE_MANIFEST_FILE);
        byte[] signatureData = loadLicense(manifestFile, ManifestAttributes.SIGNATURE);
        try {
            signatureData = decryptLicenseData(signatureData);
        } catch (Exception e) {
            throw new LicenseException("MRC 未授权：" + e.getMessage());
        }
        
        ModuleSignature signature = new ModuleSignature(module);
        signature.validate(signatureData);
        
        module.setAttribute("*", licenseData);
    }

}
