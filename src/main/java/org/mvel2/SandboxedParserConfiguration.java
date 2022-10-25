package org.mvel2;

import org.mvel2.compiler.AbstractParser;

import java.util.Map;
import java.util.stream.Collectors;

public class SandboxedParserConfiguration extends ParserConfiguration {

    private SandboxedClassLoader sanboxedClassLoader = new SandboxedClassLoader();

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
    public Object getStaticOrClassImport(String name) {
        return imports.getOrDefault(name, null);
    }

    public void addAllowedPackage(String packageName) {
        super.addPackageImport(packageName);
        this.sanboxedClassLoader.addAllowedPackage(packageName);
    }
}
