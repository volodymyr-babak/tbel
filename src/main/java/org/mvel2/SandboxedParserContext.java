package org.mvel2;

import java.lang.reflect.Method;
import java.util.Map;

public class SandboxedParserContext extends ParserContext {

    public SandboxedParserContext(SandboxedParserConfiguration sandboxedParserConfiguration) {
        super(sandboxedParserConfiguration);
    }

    @Override
    public boolean hasLiteral(String property) {
        return SandboxedParserConfiguration.literals.containsKey(property);
    }

    @Override
    public Object getLiteral(String property) {
        return SandboxedParserConfiguration.literals.get(property);
    }

    @Override
    public void setLiterals(Map<String, Object> literals) {
        // Do nothing
    }

    @Override
    public boolean isMethodAllowed(Method method) {
        return !SandboxedClassLoader.forbiddenMethods.contains(method);
    }

}
