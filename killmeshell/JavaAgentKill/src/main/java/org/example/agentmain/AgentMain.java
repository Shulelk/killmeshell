package org.example.agentmain;


import javassist.*;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;

import java.io.IOException;
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
                    inst.addTransformer(new CheckEvilTransformer(), true);
                    inst.retransformClasses(cls);
                    continue;
                }
                Class[] interfaces = cls.getInterfaces();
                for (Class inter : interfaces) {
                    String interfaceName = inter.getName();
                    if (interfaceName.equals("javax.servlet.ServletRequestListener") || interfaceName.equals("jakarta.servlet.ServletRequestListener")
                            || interfaceName.equals("javax.servlet.Servlet") || interfaceName.equals("jakarta.servlet.Servlet")
                            || interfaceName.equals("org.apache.catalina.Valve") || interfaceName.equals("org.apache.catalina.Container")
                            || interfaceName.equals("javax.servlet.Filter") || interfaceName.equals("jakarta.servlet.Filter") || interfaceName.equals("org.springframework.web.servlet.HandlerInterceptor")) {
                        inst.addTransformer(new CheckEvilTransformer(), true);
                        inst.retransformClasses(cls);
                        break;
                    }
                }
            }
        }
    }

    public static String getDefaultReturnValue(CtClass returnType) {
        if (returnType == CtClass.booleanType) {
            return "false";
        } else if (returnType == CtClass.byteType) {
            return "(byte) 0";
        } else if (returnType == CtClass.charType) {
            return "'\\0'";
        } else if (returnType == CtClass.shortType) {
            return "(short) 0";
        } else if (returnType == CtClass.intType) {
            return "0";
        } else if (returnType == CtClass.longType) {
            return "0L";
        } else if (returnType == CtClass.floatType) {
            return "0.0f";
        } else if (returnType == CtClass.doubleType) {
            return "0.0d";
        } else {
            return "null"; // 对象类型返回 null
        }
    }

    static class CheckEvilTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            CtClass ctClass = null;
            try {
                //获取CtClass 对象的容器 ClassPool
                ClassPool classPool = ClassPool.getDefault();
                //添加额外的类搜索路径
                if (classBeingRedefined != null) {
                    ClassClassPath ccp = new ClassClassPath(classBeingRedefined);
                    classPool.insertClassPath(ccp);
                }
                //获取目标类
                ctClass = classPool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer), false);
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
                            if (op == Opcode.INVOKESTATIC || op == Opcode.INVOKEVIRTUAL || op == Opcode.INVOKESPECIAL) {
                                int constPoolIndex = codeIterator.u16bitAt(index + 1);
                                String invokedMethod = methodInfo.getConstPool().getMethodrefClassName(constPoolIndex) + "."
                                        + methodInfo.getConstPool().getMethodrefName(constPoolIndex);
                                if (invokedMethod.contains("Runtime") || invokedMethod.contains("exec") || invokedMethod.contains("ProcessBuilder")
                                        || invokedMethod.contains("Class.forName") || invokedMethod.contains("ClassLoader.defineClass")
                                ) {
                                    String body;
                                    CtClass returnType = ctMethod.getReturnType();
                                    if (returnType == CtClass.voidType) {
                                        if (ctMethod.getName().equals("doFilter")) {
                                            body = "{$3.doFilter($1,$2);}";
                                        } else {
                                            body = "{System.out.println(\"Hacker!\");}";
                                        }
                                    } else if (returnType.isPrimitive()) {
                                        String defaultValue = getDefaultReturnValue(returnType);
                                        body = "{ return " + defaultValue + "; }";
                                    } else {
                                        body = "{ return null; }";
                                    }
                                    ctMethod.setBody(body);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                return ctClass.toBytecode();
            } catch (IOException | CannotCompileException e) {
                return null;
            }
        }
    }
}
