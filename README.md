## killmeshell

一个查杀 Java 内存马的简单 Demo，支持 jdk8 与 jdk17

核心用法：

```
java -cp "tools.jar;conn.jar" org.example.Main Agent.jar target
```

* `Agent.jar`：您可以选择使用 `GetClassAgent.jar` 或 `JavaAgentKill.jar`。

* `target`：目标类的全限定类名。可以用 `jps -l` 查看

其中

GetClassAgent.jar 用于获取 Servlet、Filter、Listener、Valve、WebSocket 的类文件，将加载的类文件写到目标 Web 的 result 目录下

JavaAgentKill.jar 用于自动清理上述提到的组件的内存马

jars 目录下的 jar 包是使用 jdk8 编译。

### 构建项目

```
cd Connectjps/ # 注意改 pom.xml 中 tools.jar 的路径
mvn clean compile assembly:single
cd ../GetClassAgent
mvn package
cd ../JavaAgentKill
mvn package
```



