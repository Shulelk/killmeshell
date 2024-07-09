package org.example.agentmain;


import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public class WriteClassFile {
    public static void agentmain(String args, Instrumentation inst) throws UnmodifiableClassException {
        Class[] classes = inst.getAllLoadedClasses();
        for (Class cls : classes) {
            Class superclass = cls.getSuperclass();

            if (superclass != null) {
                String superclassName = superclass.getName();
                if (superclassName.contains("HttpServlet") || superclassName.contains("ws.Endpoint")) {
                    inst.addTransformer(new WriteClassTransformer(), true);
                    inst.retransformClasses(cls);
                    continue;
                }
            }

            Class[] interfaces = cls.getInterfaces();
            for (Class inter : interfaces) {
                String interfaceName = inter.getName();
                if (interfaceName.equals("javax.servlet.ServletRequestListener") || interfaceName.equals("jakarta.servlet.ServletRequestListener")
                        || interfaceName.equals("javax.servlet.Servlet") || interfaceName.equals("jakarta.servlet.Servlet")
                        || interfaceName.equals("org.apache.catalina.Valve") || interfaceName.equals("org.apache.catalina.Container")
                        || interfaceName.equals("javax.servlet.Filter") || interfaceName.equals("jakarta.servlet.Filter") || interfaceName.equals("org.springframework.web.servlet.HandlerInterceptor")) {
                    inst.addTransformer(new WriteClassTransformer(), true);
                    inst.retransformClasses(cls);
                    break;
                }
            }
        }
    }
    static class WriteClassTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                //获取CtClass 对象的容器 ClassPool
                ClassPool classPool = ClassPool.getDefault();

                //添加额外的类搜索路径
                if (classBeingRedefined != null) {
                    ClassClassPath ccp = new ClassClassPath(classBeingRedefined);
                    classPool.insertClassPath(ccp);
                }
                //获取目标类
                CtClass ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer),false);
                String dirName = "results";
                File directory = new File(dirName);
                if (!directory.exists()) {
                    if (directory.mkdir()) {
                        ctClass.writeFile(dirName);
                    }
                } else {
                    ctClass.writeFile(dirName);
                }
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
