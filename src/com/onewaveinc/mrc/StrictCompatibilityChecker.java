package com.onewaveinc.mrc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * 严格的模块相容性检查器
 * 
 * @author gmice
 */
class StrictCompatibilityChecker {
    
    private static final Logger logger = Logger.getLogger(StrictCompatibilityChecker.class.getName());
    
    private ModuleContext moduleContext;
    
    StrictCompatibilityChecker(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }
    
    void execute() throws ModuleException, UnsatisfiedDependencyException {
        /**
         * 构造可用模块 Map，键为模块 ID，值为最高级别最高版本的模块
         * 构造可用扩展模块 Map，键为被扩展模块 ID，值为最高级别最高版本的模块
         * 同时，对于重复 ID 的模块，停用级别低的和版本低的，以达到覆盖的效果
         */
        final Map<String,Module> availableModules = new HashMap<String,Module>();
        final Map<String,Module> availableExtensionModules = new HashMap<String,Module>();
        try {
            moduleContext.visitModules(new ModuleVisitor() {
                public void visit(Module module) throws Exception {
                    final String moduleId = module.getId();
                    final Module existModule = availableModules.get(moduleId);
                    if (existModule == null) {
                        availableModules.put(moduleId, module);
                    } else {
                        // 首先检查级别，停用级别低的模块
                        int c = module.getLevel() - existModule.getLevel();
                        if (c == 0) {
                            // 其次检查版本，停用版本低的模块
                            c = module.getVersion().compareTo(existModule.getVersion());
                        }
                        
                        if (c == 0) {
                            // 模块 ID，版本，级别都相同，则认为是重复模块
                            throw new ModuleException("重复模块: " + module);
                        }
                        
                        if (c > 0) {
                            availableModules.put(moduleId, module);
                            existModule.setEnabled(false);
                        } else {
                            module.setEnabled(false);
                        }
                    }
                    
                    if (module.getExtend() == null) return;
                    
                    // 处理扩展模块
                    final String moduleExtend = module.getExtend(); 
                    final Module existExtensionModule = availableExtensionModules.get(moduleExtend);
                    if (existExtensionModule == null) {
                        availableExtensionModules.put(moduleExtend, module);
                    } else {
                        // 首先检查级别，停用级别低的模块
                        int c = module.getLevel() - existModule.getLevel();
                        if (c == 0) {
                            if (moduleId.equals(existExtensionModule.getId())) {
                                // 其次检查版本，停用版本低的模块。注意此时模块 ID 必须相同
                                c = module.getVersion().compareTo(existModule.getVersion());
                            } else {
                                // 模块级别相同但 ID 不同，此时无法决定使用哪个，抛出异常
                                throw new ModuleException("重复模块扩展。模块: " + existExtensionModule + " 和模块: " + module + " 都扩展了同一模块: " + moduleExtend);
                            }
                        }
                        
                        if (c == 0) {
                            // 模块 ID，版本，级别都相同，则认为是重复模块
                            throw new ModuleException("重复模块: " + module);
                        }
                    }
                }
            });
        } catch (ModuleException e) {
            throw e;
        } catch (Exception e) {
            throw new ModuleException("构造可用模块 Map 时出错", e);
        }
        
        /**
         * 检查扩展模块所需的被扩展模块是否存在。若不存在，则停用扩展模块并给出警告
         */
        boolean changed;
        do {
            changed = false;
            for (Entry<String,Module> entry : availableExtensionModules.entrySet()) {
                // 判断被扩展模块是否存在
                if (availableModules.containsKey(entry.getKey())) continue;
                
                Module extensionModule = entry.getValue();
                if (extensionModule.isEnabled()) {
                    extensionModule.setEnabled(false);
                    
                    logger.warning(String.format("扩展模块 %s 被停用，因为所需的被扩展模块 %s 不存在", extensionModule, entry.getKey()));
                    
                    availableModules.remove(extensionModule.getId());
                    changed = availableExtensionModules.containsKey(extensionModule.getId());
                }
            }
        } while (changed);
        
        // 遍历所有启用的模块，处理 module.require
        try {
            moduleContext.visitModules(new ModuleVisitor() {
                public void visit(final Module module) throws Exception {
                    List<RequireEntry> require = module.getRequire();
                    if (require == null || require.isEmpty()) return;
                    
                    for (final RequireEntry requireEntry : require) {
                        final String id = requireEntry.getId();
                        final VersionRange versionRange = requireEntry.getVersionRange();
                        final Module requireModule = availableModules.get(id);

                        // 若不存在依赖模块，则依赖检查失败
                        if (requireModule == null) {
                            throw new UnsatisfiedDependencyException(String.format("模块 %s 依赖的模块 %s 不存在或已被其他规则排除", module, requireEntry));
                        }
                        
                        // 若依赖模块版本不在声明的版本范围内，则依赖检查失败
                        if (!versionRange.contains(requireModule.getVersion())) {
                            throw new UnsatisfiedDependencyException(String.format("模块 %s 声明的依赖模块 %s 的版本范围为: %s，但是现有模块 %s 的版本为: %s", module, id, versionRange, id, requireModule.getVersion()));
                        }
                    }
                }
            });
        } catch (UnsatisfiedDependencyException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsatisfiedDependencyException("依赖检查失败", e);
        }
    }

}
