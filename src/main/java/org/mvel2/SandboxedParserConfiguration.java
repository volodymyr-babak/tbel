package org.mvel2;

import org.mvel2.compiler.AbstractParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SandboxedParserConfiguration extends ParserConfiguration {

    private final Map<Class<?>, Function<Object, Long>> additionalDataTypes = new HashMap<>();

    private SandboxedClassLoader sanboxedClassLoader = new SandboxedClassLoader();

    protected static final Map<String, Object> literals = AbstractParser.LITERALS
            .entrySet().stream().filter(entry -> !SandboxedClassLoader.forbiddenClassLiterals.contains(entry.getKey()))
            .collect(HashMap::new, (m, v)->m.put(v.getKey(), v.getValue()), HashMap::putAll);

    public SandboxedParserConfiguration() {
        setClassLoader(sanboxedClassLoader);
        setImports(AbstractParser.CLASS_LITERALS
                .entrySet().stream().filter(entry -> !SandboxedClassLoader.forbiddenClassLiterals.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public boolean hasImport(String name) {
        return imports.containsKey(name);
    }

    @Override
    public Class getImport(String name) {
        if (imports.containsKey(name) && imports.get(name) instanceof Class) {
            return (Class) imports.get(name);
        }
        return null;
    }

    @Override
    public void addImport(String name, Class cls) {
        super.addImport(name, cls);
        sanboxedClassLoader.addAllowedClass(cls);
    }

    @Override
    public Object getStaticOrClassImport(String name) {
        return imports.getOrDefault(name, null);
    }

    public void addAllowedPackage(String packageName) {
        super.addPackageImport(packageName);
        this.sanboxedClassLoader.addAllowedPackage(packageName);
    }

    @SuppressWarnings("unchecked")
    public <T> void registerDataType(String name, Class<T> cls, Function<T, Long> valueSizeFunction) {
        this.addImport(name, cls);
        this.additionalDataTypes.put(cls, (Function<Object, Long>) valueSizeFunction);
    }

    public Function<Object, Long> getValueSizeFunction(Class<?> cls) {
        return this.additionalDataTypes.get(cls);
    }
}
