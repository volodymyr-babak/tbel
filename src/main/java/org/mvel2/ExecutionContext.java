package org.mvel2;

import java.io.Serializable;

public class ExecutionContext implements Serializable {

    private volatile boolean stopped = false;

    public ExecutionContext() {
    }

    public void checkExecution() {
        if (stopped) {
            throw new ScriptRuntimeException("Script execution is stopped!");
        }
    }

    public void stop() {
        this.stopped = true;
    }
}
