package org.example.agentmain;


import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public class AgentMain {
    public static void agentmain(String args, Instrumentation inst) throws UnmodifiableClassException {
        Class[] classes = inst.getAllLoadedClasses();
        for (Class cls : classes) {
            Class superclass = cls.getSuperclass();
            if (superclass != null) {
                String superclassName = superclass.getName();
                if (superclassName.contains("HttpServlet") || superclassName.contains("ws.Endpoint")) {
                    inst.addTransformer(new EvilServletTransformer(), true);
                    inst.retransformClasses(cls);
                    continue;
                }

                Class[] interfaces = cls.getInterfaces();
                for (Class inter : interfaces) {
                    String interfaceName = inter.getName();
                    if (interfaceName.equals("javax.servlet.ServletRequestListener") || interfaceName.equals("jakarta.servlet.ServletRequestListener")
                            || interfaceName.equals("javax.servlet.Servlet") || interfaceName.equals("jakarta.servlet.Servlet")
                            || interfaceName.equals("org.apache.catalina.Valve") || interfaceName.equals("org.apache.catalina.Container")) {
                        inst.addTransformer(new EvilServletTransformer(), true);
                        inst.retransformClasses(cls);
                        break;
                    }
                    if (interfaceName.equals("javax.servlet.Filter") || interfaceName.equals("jakarta.servlet.Filter")) {
                        inst.addTransformer(new EvilFilterTransformer(), true);
                        inst.retransformClasses(cls);
                        break;
                    }
                }
            }
        }
    }
    static class EvilServletTransformer implements ClassFileTransformer {
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
                CtClass ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer), false);
                //获取目标方法
                CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                for (CtMethod ctMethod : declaredMethods) {
                    MethodInfo methodInfo = ctMethod.getMethodInfo();
                    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
                    if (codeAttribute != null) {
                        CodeIterator codeIterator = codeAttribute.iterator();
                        while (codeIterator.hasNext()) {
                            int index = codeIterator.next();
                            int op = codeIterator.byteAt(index);
                            // 检查是否调用了 Runtime.getRuntime().exec()
                            if (op == Opcode.INVOKESTATIC) {
                                int constPoolIndex = codeIterator.u16bitAt(index + 1);
                                String invokedMethod = methodInfo.getConstPool().getMethodrefClassName(constPoolIndex) + "." + methodInfo.getConstPool().getMethodrefName(constPoolIndex);
                                //System.out.println(invokedMethod);
                                if (invokedMethod.contains("getRuntime") || invokedMethod.contains("Runtime") || invokedMethod.contains("exec")) {
                                    System.out.println("Found invocation of Runtime.getRuntime().exec() in " + className);
                                    //设置方法体
                                    String body = "{System.out.println(\"Hacker!\");}";
                                    ctMethod.setBody(body);
                                    //返回目标类字节码
                                    return ctClass.toBytecode();
                                }
                            }

                            if (op ==  Opcode.INVOKEVIRTUAL) {
                                int constPoolIndex = codeIterator.u16bitAt(index + 1);
                                String className2 = methodInfo.getConstPool().getMethodrefClassName(constPoolIndex);
                                if (className2.contains("ProcessBuilder")) {
                                    System.out.println("Found invocation of ProcessBuilder in " + className);
                                    //设置方法体
                                    String body = "{System.out.println(\"Hacker!\");}";
                                    ctMethod.setBody(body);

                                    //返回目标类字节码
                                    return ctClass.toBytecode();
                                }
                            }

                        }
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class EvilFilterTransformer implements ClassFileTransformer {
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
                //获取目标方法
                CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                for (CtMethod ctMethod : declaredMethods) {
                    MethodInfo methodInfo = ctMethod.getMethodInfo();
                    CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
                    if (codeAttribute != null) {
                        CodeIterator codeIterator = codeAttribute.iterator();
                        while (codeIterator.hasNext()) {
                            int index = codeIterator.next();
                            int op = codeIterator.byteAt(index);
                            // 检查是否调用了 Runtime.getRuntime().exec()
                            if (op == Opcode.INVOKESTATIC) {
                                int constPoolIndex = codeIterator.u16bitAt(index + 1);
                                String invokedMethod = methodInfo.getConstPool().getMethodrefClassName(constPoolIndex) + "."
                                        + methodInfo.getConstPool().getMethodrefName(constPoolIndex);
                                if (invokedMethod.contains("getRuntime") || invokedMethod.contains("Runtime") || invokedMethod.contains("exec")) {
                                    System.out.println("Found invocation of Runtime.getRuntime().exec() in " + className + "." + "service");
                                    //设置方法体
                                    String body;
                                    if (ctMethod.getName().equals("doFilter")) {
                                        body = "{$3.doFilter($1,$2);}";
                                    } else {
                                        body = "{System.out.println(\"Hacker!\");}";
                                    }

                                    ctMethod.setBody(body);
                                    //返回目标类字节码
                                    return ctClass.toBytecode();
                                }
                            }
                            if (op ==  Opcode.INVOKEVIRTUAL) {
                                int constPoolIndex = codeIterator.u16bitAt(index + 1);
                                String className2 = methodInfo.getConstPool().getMethodrefClassName(constPoolIndex);
                                if (className2.contains("ProcessBuilder")) {
                                    System.out.println("Found invocation of ProcessBuilder in " + className);
                                    String body;
                                    if (ctMethod.getName().equals("doFilter")) {
                                        body = "{$3.doFilter($1,$2);}";
                                    } else {
                                        body = "{System.out.println(\"Hacker!\");}";
                                    }
                                    ctMethod.setBody(body);
                                    //返回目标类字节码
                                    return ctClass.toBytecode();
                                }
                            }
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
