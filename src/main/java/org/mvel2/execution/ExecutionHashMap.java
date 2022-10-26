package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.HashMap;

public class ExecutionHashMap<K,V> extends HashMap<K,V> implements ExecutionObject {

    private final ExecutionContext executionContext;

    private final int id;

    private long memorySize = 0;

    public ExecutionHashMap(int size, ExecutionContext executionContext) {
        super(size);
        this.executionContext = executionContext;
        this.id = executionContext.nextId();
    }

    @Override
    public V put(K key, V value) {
        if (containsKey(key)) {
            V prevValue = this.get(key);
            this.memorySize -= this.executionContext.onValRemove(this, key, prevValue);
        }
        V res = super.put(key, value);
        this.memorySize += this.executionContext.onValAdd(this, key, value);
        return res;
    }

    @Override
    public V remove(Object key) {
        if (containsKey(key)) {
            V value = this.get(key);
            this.memorySize -= this.executionContext.onValRemove(this, key, value);
        }
        return this.remove(key);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public long memorySize() {
        return memorySize;
    }

    @Override
    public String toString() {
        String res = super.toString();
        return "(id="+id+") " + res;
    }
}
