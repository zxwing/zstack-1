package org.zstack.storage.boss;

import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by XXPS-PC1 on 2016/11/10.
 */
public class ExecuteShellCommand {

    private int exitValue = 1;

    public ExecuteShellCommand(){
    }

    public int getExitValue() { return exitValue;}

    public String executeCommand(String command,ErrorFacade errf) {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            this.exitValue = p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }


        } catch (Exception e) {
            this.exitValue = 2;
            throw new OperationFailureException(errf.stringToOperationError("error in execute shell command"));
        }
        return output.toString();
    }
}
