package org.mvel2;

import org.mvel2.ast.PrototypalFunctionInstance;
import org.mvel2.execution.ExecutionObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionContext implements Serializable {

    private final Map<Object, ValueReference> valueReferenceMap = new IdentityHashMap<>();
    private final Map<String, Object> variablesMap = new HashMap<>();

    private final long maxAllowedMemory;

    private long memorySize = 0;

    private final AtomicInteger idSequence = new AtomicInteger(0);

    private volatile boolean stopped = false;

    public ExecutionContext() {
        this(-1);
    }

    public ExecutionContext(long maxAllowedMemory) {
        this.maxAllowedMemory = maxAllowedMemory;
    }

    public int nextId() {
        return this.idSequence.incrementAndGet();
    }

    public void checkExecution() {
        if (stopped) {
            throw new ScriptRuntimeException("Script execution is stopped!");
        }
    }

    public void stop() {
        this.stopped = true;
    }

    public Object checkAssignVariable(String varName, Object value) {
        if (this.variablesMap.containsKey(varName)) {
            Object prevValue = this.variablesMap.get(varName);
            ValueReference reference = valueReferenceMap.get(prevValue);
            if (reference != null) {
                if (reference.removeReference(varName)) {
                    valueReferenceMap.remove(prevValue);
                    memorySize -= reference.getSize();
                }
            }
        }
        if (value != null) {
            this.variablesMap.put(varName, value);
            ValueReference reference = valueReferenceMap.computeIfAbsent(value, o -> {
                ValueReference newReference = new ValueReference();
                newReference.setSize(getValueSize(value));
                memorySize += newReference.getSize();
                return newReference;
            });
            reference.addReference(varName);
        } else {
            this.variablesMap.remove(varName);
        }
        this.checkMemoryLimit();
        return value;
    }

    public long onValRemove(ExecutionObject obj, Object key, Object val) {
        long valSize = getValueSize(key) + getValueSize(val);
        ValueReference reference = valueReferenceMap.get(obj);
        if (reference != null) {
            reference.setSize(reference.getSize() - valSize);
        }
        memorySize -= valSize;
        return valSize;
    }

    public long onValAdd(ExecutionObject obj, Object key, Object val) {
        long valSize = getValueSize(key) + getValueSize(val);
        ValueReference reference = valueReferenceMap.get(obj);
        if (reference != null) {
            reference.setSize(reference.getSize() + valSize);
        }
        memorySize += valSize;
        this.checkMemoryLimit();
        return valSize;
    }

    public void dumpVars() {
        System.out.println("VARS:");
        variablesMap.forEach((key, value) -> System.out.println(key + " = " + value));
    }

    public void dumpValueReferences() {
        System.out.println("VALUE REFERENCES:");
        valueReferenceMap.forEach((key, value) -> System.out.println(key + " = " + value));
    }

    public long getMemorySize() {
        return memorySize;
    }

    private void checkMemoryLimit() {
        if (maxAllowedMemory > 0 && memorySize > maxAllowedMemory) {
            throw new ScriptMemoryOverflowException("Script memory overflow (" + memorySize + " > " + maxAllowedMemory + ")!");
        }
    }

    private long getValueSize(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof ExecutionObject) {
            if (valueReferenceMap.containsKey(value)) {
                return 4;
            } else {
                return ((ExecutionObject) value).memorySize();
            }
        } else if (value instanceof String) {
            return ((String) value).getBytes().length;
        } else if (value instanceof Integer) {
            return 4;
        } else if (value instanceof Long) {
            return 8;
        } else if (value instanceof Float) {
            return 4;
        } else if (value instanceof Double) {
            return 8;
        } else if (value instanceof Boolean) {
            return 1;
        } else if (value instanceof Byte) {
            return 1;
        } else if (value instanceof UUID) {
            return 16;
        } else {
            throw new ScriptRuntimeException("Unsupported value type: " + value.getClass());
        }
    }

    private static final class ValueReference {
        private final Set<String> references = new HashSet<>();
        private long size = 0;

        void addReference(String varName) {
            references.add(varName);
        }

        boolean removeReference(String varName) {
            references.remove(varName);
            return references.isEmpty();
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "ValueReference[size: " + size + "; references: " + references + "]";
        }
    }
}
