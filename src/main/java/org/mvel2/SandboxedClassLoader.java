package org.mvel2;

import org.mvel2.compiler.AbstractParser;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SandboxedClassLoader extends URLClassLoader {

    protected static final Set<String> forbiddenClassLiterals =
            Set.of("System",  "Runtime", "Class", "ClassLoader", "Thread", "Compiler", "ThreadLocal", "SecurityManager");

    private final Set<String> allowedClasses = new HashSet<>();
    private final Set<String> allowedPackages = new HashSet<>();

    public SandboxedClassLoader() {
        super(new URL[0], Thread.currentThread().getContextClassLoader());
        allowedPackages.add("org.mvel2");
        allowedPackages.add("java.util");
        AbstractParser.CLASS_LITERALS
                .entrySet().stream().filter(entry -> !forbiddenClassLiterals.contains(entry.getKey()))
                .map(Map.Entry::getValue).forEach(val -> allowedClasses.add(((Class) val).getName()));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!classNameAllowed(name)) {
            throw new ClassNotFoundException();
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!classNameAllowed(name)) {
            throw new ClassNotFoundException();
        }
        return super.loadClass(name);
    }

    public void addAllowedClass(Class cls) {
        this.allowedClasses.add(cls.getName());
    }

    public void addAllowedPackage(String packageName) {
        this.allowedPackages.add(packageName);
    }

    private boolean classNameAllowed(String name) {
        if (allowedClasses.contains(name)) {
            return true;
        }
        for (String pkgName : allowedPackages) {
            if (name.startsWith(pkgName)) {
                return true;
            }
        }
        return false;
    }
}
