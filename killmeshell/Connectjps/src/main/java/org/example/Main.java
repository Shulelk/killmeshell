package org.example;

import com.sun.tools.attach.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Shule
 * CreateTime: 2024/7/4 10:15
 */
public class Main {
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        List<VirtualMachineDescriptor> list = VirtualMachine.list();
        if (args.length < 2) {
            System.out.println("usage: java -cp \"tools.jar;conn.jar\" org.example.Main Agent.jar target");
            System.exit(0);
        }
        for (VirtualMachineDescriptor virtualMachineDescriptor :list) {
            if (virtualMachineDescriptor.displayName().equals(args[1])) {
                String id = virtualMachineDescriptor.id();
                VirtualMachine attach = VirtualMachine.attach(id);
                File file = new File(args[0]);
                if (file.exists()) {
                    String fileName =  file.getAbsolutePath();
                    attach.loadAgent(fileName);
                    attach.detach();
                }else {
                    System.out.println("Agent.jar not found");
                    System.exit(0);
                }
            }
        }
    }
}