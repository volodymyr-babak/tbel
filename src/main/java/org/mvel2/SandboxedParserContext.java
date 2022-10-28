package org.mvel2;

import org.mvel2.ast.Proto;
import org.mvel2.util.MethodStub;

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

    @Override
    public void addImport(Class cls) {
        throw new UnsupportedOperationException("Import is forbidden!");
    }

    @Override
    public void addImport(Proto proto) {
        throw new UnsupportedOperationException("Import is forbidden!");
    }

    @Override
    public void addImport(String name, Class cls) {
        throw new UnsupportedOperationException("Import is forbidden!");
    }

    @Override
    public void addImport(String name, Method method) {
        throw new UnsupportedOperationException("Import is forbidden!");
    }

    @Override
    public void addImport(String name, MethodStub method) {
        throw new UnsupportedOperationException("Import is forbidden!");
    }


}
