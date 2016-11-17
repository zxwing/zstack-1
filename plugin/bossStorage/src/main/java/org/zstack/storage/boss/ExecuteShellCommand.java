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
    private StringBuffer exeError = null;

    public ExecuteShellCommand(){
    }

    public int getExitValue() { return exitValue;}

    public String executeCommand(String command,ErrorFacade errf) {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            String []cmdArray = new String[]{"/bin/bash","-c",command};
            p = Runtime.getRuntime().exec(cmdArray);
            this.exitValue = p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader reader2 =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }


            while ((line = reader2.readLine())!= null) {
                exeError.append(line + "\n");
            }


        } catch (Exception e) {
            this.exitValue = 2;
            throw new OperationFailureException(errf.stringToOperationError("error in execute shell command"));
        }
        return output.toString();
    }
}
