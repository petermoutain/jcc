package com.onewaveinc.mrc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MRC 全局类装载器
 * 
 * @author gmice
 * 
 */
public class ModuleContextClassLoader extends URLClassLoader {

    private static final Logger logger = Logger.getLogger(ModuleContextClassLoader.class.getName());

    private ClassLoader appClassLoader;

    private ClassLoader extClassLoader;

    private ModuleContext moduleContext;

    private Map<String,URL> resourceURLs;
    
    private ProtectionDomain protectionDomain;
    
    private List<ModuleContextClassLoaderFilter> filters;

    public ModuleContextClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public ModuleContextClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.appClassLoader = parent;
        this.extClassLoader = parent.getParent();
        this.protectionDomain = this.getClass().getProtectionDomain();
    }

    public void addURLs(List<URL> urls) {
        for (URL url : urls) {
            addURL(url);
        }
    }
    
    public void addFilter(ModuleContextClassLoaderFilter filter) {
        if (filters == null) {
            filters = new ArrayList<ModuleContextClassLoaderFilter>();
        }
        filters.add(filter);
    }

    @Override
    public URL findResource(String name) {
        URL url = resourceURLs == null ? null : resourceURLs.get(name);
        if (url == null) {
            url = super.findResource(name);
        }
        return url;
    }

    public void setModuleContext(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    public void setResourceURLs(Map<String,URL> resourceURLs) {
        this.resourceURLs = resourceURLs;
    }

    private Class<?> defineClass(String name, URL url) throws Exception {
        // 如果类从jar包中装载，则定义相应的package
        if ("jar".equals(url.getProtocol())) {
            int index = name.lastIndexOf('.');
            if (index != -1) {
                String packageName = name.substring(0, index);
                Package pkg = getPackage(packageName);
                if (pkg == null) {
                    Manifest manifest = ((JarURLConnection) url.openConnection()).getManifest();
                    if (manifest != null) {
                        definePackage(packageName, manifest, url);
                    } else {
                        definePackage(packageName, null, null, null, null, null, null, null);
                    }
                }
            }
        }
        
        // 读取类数据
        InputStream in = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int c = 0;
        while ((c = in.read(buf)) != -1) {
            out.write(buf, 0, c);
        }
        
        // 通过过滤器进行可能的字节码修改
        byte[] classData = out.toByteArray();
        if (filters != null) {
            for (ModuleContextClassLoaderFilter filter : filters) {
                classData = filter.doFilter(name, classData);
            }
        }
        
        // 定义类
        Class<?> clazz = defineClass(name, classData, 0, classData.length, protectionDomain);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "类装载 [mrc] " + name);
        }
        return clazz;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 1. 对已经载入的类，直接返回
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) return clazz;

        // 2. 能用 ext classloader 载入的类，就直接用 ext classloader 载入。这些类是 jre 自带的，在 mrc classloader 中载入
        try {
            clazz = extClassLoader.loadClass(name);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "类装载 [ext] " + name);
            }
        } catch (ClassNotFoundException ignore) {}

        // 3. MRC 核心部分的类由 app classloader 载入。除此之外，其他类由 mrc classloader 自己载入
        if (clazz == null && name.startsWith("com.onewaveinc.mrc.")) {
            clazz = appClassLoader.loadClass(name);
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "类装载 [app] " + name);
            }
        }

        if (clazz == null) {
            // 4. MRC 尚未启动，说明自己是 system classloader，那么启动一个 MRC
            if (moduleContext == null) {
                File rootDirectory = new File(System.getProperty("user.dir"));
                File mipDirectory = new File(rootDirectory, ".mip");
                // 如果工作目录下存在 .mip 目录，则 MRC 根目录设为 .mip 目录，否则设为工作目录
                moduleContext = new ModuleContext((mipDirectory.isDirectory() ? mipDirectory : rootDirectory).getAbsolutePath());
                moduleContext.run();
            }

            // 5. 载入类
            URL url = getResource(name.replace('.', '/') + ".class");
            if (url != null) {
                try {
                    clazz = defineClass(name, url);
                } catch (Throwable e) { // 这里使用Throwable是因为类似于UnsupportedClassVersionError的Error也需要捕获
                    throw new ClassNotFoundException(name, e);
                }
            }
        }

        // 6. 链接类
        if (clazz != null) {
            if (resolve) resolveClass(clazz);
            return clazz;
        }

        throw new ClassNotFoundException(name);
    }

}
