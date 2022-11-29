package org.mvel2;

import org.mvel2.ast.Proto;
import org.mvel2.util.MethodStub;

import java.lang.reflect.Method;
import java.util.Map;

public class SandboxedParserContext extends ParserContext {

    public SandboxedParserContext(SandboxedParserConfiguration sandboxedParserConfiguration) {
        super(sandboxedParserConfiguration);
    }

    public SandboxedParserContext(SandboxedParserConfiguration sandboxedParserConfiguration, ParserContext parent, boolean functionContext) {
        super(sandboxedParserConfiguration, parent, functionContext);
    }

    @Override
    public ParserContext createFunctionContext(ParserConfiguration parserConfiguration) {
        return new SandboxedParserContext((SandboxedParserConfiguration) parserConfiguration, this, true);
    }

    @Override
    protected ParserContext createNewParserContext(ParserConfiguration parserConfiguration) {
        return new SandboxedParserContext((SandboxedParserConfiguration) parserConfiguration);
    }

    @Override
    protected ParserContext prepareColoringSubcontext(ParserConfiguration parserConfiguration, ParserContext _parent) {
        return new SandboxedParserContext((SandboxedParserConfiguration) parserConfiguration) {
            @Override
            public void addVariable(String name, Class type) {
                if ((_parent.variables != null && _parent.variables.containsKey(name))
                        || (_parent.inputs != null && _parent.inputs.containsKey(name))) {
                    this.variablesEscape = true;
                }
                super.addVariable(name, type);
            }

            @Override
            public void addVariable(String name, Class type, boolean failIfNewAssignment) {
                if ((_parent.variables != null && _parent.variables.containsKey(name))
                        || (_parent.inputs != null && _parent.inputs.containsKey(name))) {
                    this.variablesEscape = true;
                }
                super.addVariable(name, type, failIfNewAssignment);
            }

            @Override
            public Class getVarOrInputType(String name) {
                if ((_parent.variables != null && _parent.variables.containsKey(name))
                        || (_parent.inputs != null && _parent.inputs.containsKey(name))) {
                    this.variablesEscape = true;
                }

                return super.getVarOrInputType(name);
            }
        };
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
