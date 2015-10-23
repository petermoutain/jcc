package com.onewaveinc.mrc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于在命令行方式启动 MRC 并执行指定类的 main 方法
 * 
 * @author gmice
 */
public class Runner {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("启动MRC，在MRC环境中执行指定类的main()");
            System.out.println();
            System.out.println("Runner [选项] main类 [参数...]");
            System.out.println("  -f           即使MRC启动失败，也会继续执行main()");
            System.out.println("  -k           main()返回后，不关闭MRC");
            System.out.println("  -n           仅启动/关闭MRC，不执行main()");
            System.out.println("  -Dkey=value  设置默认的MRC属性");
            System.exit(1);
        }

        String mainClass = null;
        List<String> options = new ArrayList<String>();
        for (String arg : args) {
            if (arg.startsWith("-")) {
                options.add(arg);
                continue;
            }
            mainClass = arg;
            break;
        }

        final ModuleContext moduleContext = new ModuleContext(System.getProperty("user.dir"));

        Pattern pattern = Pattern.compile("^-D([^=]+)=([^=]+)$");
        for (String option : options) {
            Matcher matcher = pattern.matcher(option);
            if (matcher.matches()) {
                moduleContext.setProperty(matcher.group(1), matcher.group(2));
            }
        }

        moduleContext.run();
        
        try {
            if (!moduleContext.isRunning() && !options.contains("-f")) return; 
            
            if (!options.contains("-n")) {
                if (mainClass == null) {
                    System.err.println("未指定main类");
                    System.exit(1);
                }
                
                Class<?> clazz = moduleContext.getClassLoader().loadClass(mainClass);
    
                // 当类有setModuleContext方法时，注入ModuleContext
                try {
                    Method setModuleContext = clazz.getMethod("setModuleContext", new Class<?>[] { ModuleContext.class });
                    if (Modifier.isStatic(setModuleContext.getModifiers())) {
                        setModuleContext.invoke(null, new Object[] { moduleContext });
                    }
                } catch (NoSuchMethodException ignore) {}
    
                Method main = clazz.getMethod("main", new Class<?>[] { String[].class });
                String[] argsPassedIn = new String[args.length - 1];
                System.arraycopy(args, 1, argsPassedIn, 0, argsPassedIn.length);
                
                ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(moduleContext.getClassLoader());
                    main.invoke(null, new Object[] { argsPassedIn });
                } finally {
                    Thread.currentThread().setContextClassLoader(originClassLoader);
                }
            }
        } finally {
            if (!options.contains("-k")) {
                moduleContext.stop();
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    public void run() {
                        moduleContext.stop();
                    }
                }));
                
                while (moduleContext.isRunning()) {
                    Thread.sleep(1000);
                }
            }
        }
        
    }

}
