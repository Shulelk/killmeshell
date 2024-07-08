package org.example.agentmain;


import javassist.*;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public class KillSomeClass {
    public static String className;
    public static String methodName;

    public static void agentmain(String args, Instrumentation inst) throws UnmodifiableClassException, IOException {
        Class[] classes = inst.getAllLoadedClasses();
        String[] classNameAndMethodName = args.split("#");
        className = classNameAndMethodName[0];
        methodName = classNameAndMethodName[1];

        for (Class cls : classes) {
            if (cls.getName().equals(className)) {
                inst.addTransformer(new KillSomeClassTransformer(), true);
                inst.retransformClasses(cls);
                break;
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

    static class KillSomeClassTransformer implements ClassFileTransformer {
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
                CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                for (CtMethod ctMethod : declaredMethods) {
                    if (ctMethod.getName().equals(methodName)) {
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
                        break;
                    }
                }
                return ctClass.toBytecode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
