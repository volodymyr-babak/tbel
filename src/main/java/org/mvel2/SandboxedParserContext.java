package org.mvel2;

import org.mvel2.compiler.AbstractParser;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.stream.Collectors;

public class SandboxedParserContext extends ParserContext {

    private SandboxedParserConfiguration sandboxedParserConfiguration;

    public SandboxedParserContext() {
        super(new SandboxedParserConfiguration());
        this.sandboxedParserConfiguration = (SandboxedParserConfiguration) this.getParserConfiguration();
        setLiterals(AbstractParser.LITERALS
                .entrySet().stream().filter(entry -> !SandboxedClassLoader.forbiddenClassLiterals.contains(entry.getKey()))
                .collect(HashMap::new, (m, v)->m.put(v.getKey(), v.getValue()), HashMap::putAll));
        this.addImport("JSON", TbJson.class);
    }

    public void addAllowedPackage(String packageName) {
        this.sandboxedParserConfiguration.addAllowedPackage(packageName);
    }

    @Override
    public boolean isMethodAllowed(Method method) {
        return !SandboxedClassLoader.forbiddenMethods.contains(method);
    }

}
