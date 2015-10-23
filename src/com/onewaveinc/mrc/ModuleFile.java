package com.onewaveinc.mrc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 模块文件。继承自 java.io.File，增加了文件所属的模块以及文件相对于模块根目录的路径信息
 * 
 * @author gmice
 */
public class ModuleFile extends File {

    private static final Comparator<ModuleFile> comparator = new Comparator<ModuleFile>() {
        public int compare(ModuleFile o1, ModuleFile o2) {
            return o1.getModulePath().compareTo(o2.getModulePath());
        }
    };
    
    private static final long serialVersionUID = 1L;

    private Module module;

    private List<Module> moduleExtensions;
    
    private String modulePath;
    
    /**
     * 根据指定模块，创建模块文件实例对应该模块根目录
     * 
     * @param module 模块
     */
    ModuleFile(Module module) {
        this(module, "/");
    }

    /**
     * 根据指定模块和路径，创建模块文件实例
     * 
     * @param module 模块
     * @param modulePath 路径，相对于模块根目录
     */
    ModuleFile(Module module, String modulePath) {
        super(module.getRootDirectory() + modulePath);
        this.module = module;
        this.modulePath = modulePath;
        this.moduleExtensions = module.getExtensions();
    }

    ModuleFile(Module module, String modulePath, String pathname) {
        super(pathname);
        this.module = module;
        this.modulePath = modulePath;
        this.moduleExtensions = module.getExtensions();
    }
    
    /**
     * 根据指定模块目录和文件名，创建模块文件实例对应该目录下的指定文件
     * 
     * @param parent 模块文件，必须是一个目录
     * @param child 文件名
     */
    ModuleFile(ModuleFile parent, String child) {
        super(parent, child);
        this.module = parent.module;
        this.modulePath = ("/".equals(parent.modulePath) ? "" : parent.modulePath) + (child.startsWith("/") ? child : "/" + child);
        this.moduleExtensions = parent.moduleExtensions;
    }
    
    @Override
    public boolean exists() {
        if (super.exists()) return true;
        if (moduleExtensions != null) {
            for (Module extension : moduleExtensions) {
                if (new ModuleFile(extension, modulePath).exists()) return true;
            }
        }
        return false;
    }
    
    /**
     * 获得模块文件路径
     * 
     * @return 模块文件路径，相对于模块根目录
     */
    public String getModulePath() {
        return modulePath;
    }
    
    @Override
    public boolean isDirectory() {
        if (super.isDirectory()) return true;
        if (moduleExtensions != null) {
            for (Module extension : moduleExtensions) {
                if (new ModuleFile(extension, modulePath).isDirectory()) return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean isFile() {
        if (super.isFile()) return true;
        if (moduleExtensions != null) {
            for (Module extension : moduleExtensions) {
                if (new ModuleFile(extension, modulePath).isFile()) return true;
            }
        }
        return false;
    }
    
    @Override
    public ModuleFile[] listFiles() {
        if (moduleExtensions != null) {
            Map<String,ModuleFile> moduleFiles = new TreeMap<String,ModuleFile>();
            for (Module extension : moduleExtensions) {
                ModuleFile[] extensionFiles = new ModuleFile(extension, modulePath).listFilesInternal();
                if (extensionFiles != null) {
                    for (ModuleFile extensionFile : extensionFiles) {
                        if (moduleFiles.containsKey(extensionFile.modulePath)) continue;
                        moduleFiles.put(extensionFile.modulePath, extensionFile.toExternal(module));
                    }
                }
            }
            
            moduleFiles.remove(Module.METADATA_FILE);
            
            ModuleFile[] files = listFilesInternal();
            if (files != null) {
                for (ModuleFile file : files) {
                    if (!file.isDirectory() && moduleFiles.containsKey(file.modulePath)) {
                        continue;
                    }
                    moduleFiles.put(file.modulePath, file);
                }
            }
            
            return moduleFiles.values().toArray(new ModuleFile[0]);
        } else {
            return sorted(listFilesInternal());
        }
    }
    
    /**
     * 列举该实例对应的目录及其子目录下的所有模块文件（不包含目录）<br>
     * 返回的模块文件数组已根据文件路径进行了排序
     * 
     * @return 模块文件数组
     */
    public ModuleFile[] listFilesRecursively() {
        return listFilesRecursively(false);
    }
    
    /**
     * 列举该实例对应的目录及其子目录下的所有模块文件<br>
     * 返回的模块文件数组已根据文件路径进行了排序
     * 
     * @param includeDirectory 是否包括目录
     * @return 模块文件数组
     */
    public ModuleFile[] listFilesRecursively(boolean includeDirectory) {
        return sorted(listFilesRecursivelyInternal(includeDirectory));
    }
    
    private ModuleFile[] listFilesInternal() {
        File[] files = super.listFiles();
        if (files == null) return null;
        
        ModuleFile[] moduleFiles = new ModuleFile[files.length];
        for (int i = 0; i < files.length; i++) {
            moduleFiles[i] = new ModuleFile(this, "/" + files[i].getName());
        }
        return moduleFiles;
    }

    private ModuleFile[] listFilesRecursivelyInternal(boolean includeDirectory) {
        List<ModuleFile> list = new ArrayList<ModuleFile>();
        ModuleFile[] files = listFiles();
        for (ModuleFile file : files) {
            if (file.isDirectory()) {
                if (includeDirectory) list.add(file);
                list.addAll(Arrays.asList(file.listFilesRecursivelyInternal(includeDirectory)));
            } else {
                list.add(file);
            }
        }
        return list.toArray(new ModuleFile[list.size()]);
    }
    
    private ModuleFile[] sorted(ModuleFile[] files) {
        Arrays.sort(files, comparator);
        return files;
    }
    
    ModuleFile toExternal(Module module) {
        if (module == this.module) {
            return this;
        } else {
            return new ModuleFile(module, this.modulePath, getAbsolutePath());
        }
    }

}
