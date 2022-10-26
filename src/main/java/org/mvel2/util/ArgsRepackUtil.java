package org.mvel2.util;

import org.mvel2.ExecutionContext;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

public class ArgsRepackUtil {

    public static Object repack(ExecutionContext ctx, Object value){
        if (value == null) {
            return null;
        }
        if (value.getClass().isArray()) {
            ExecutionArrayList list = new ExecutionArrayList(ctx);
            int size = Array.getLength(value);
            for (int i = 0; i < size; i++) {
                list.add(repack(ctx, Array.get(value, i)));
            }
            return list;
        } else if (value instanceof Map){
            Map src = (Map)value;
            ExecutionHashMap map = new ExecutionHashMap(src.size(), ctx);
            src.forEach((k,v) -> map.put(k, repack(ctx, v)));
            return map;
        } else if (value instanceof Collection){
            ExecutionArrayList list = new ExecutionArrayList(ctx);
            for(Object o : (Collection)value){
                list.add(repack(ctx, o));
            }
            return list;
        } else {
            return value;
        }
    }
}
