package org.mvel2.tests.core;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.SandboxedParserContext;
import org.mvel2.ScriptMemoryOverflowException;
import org.mvel2.ScriptRuntimeException;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbExpressionsTest extends TestCase {

    private SandboxedParserConfiguration parserConfig;

    private ExecutionContext currentExecutionContext;

    @Override
    protected void setUp() throws Exception {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        super.setUp();
        this.parserConfig = new SandboxedParserConfiguration();
    }

    public void testCreateSingleValueArray() {
        Object res = executeScript("m = {5}; m");
        assertTrue(res instanceof List);
        assertEquals(1, ((List) res).size());
        assertEquals(5, ((List) res).get(0));
    }

    public void testCreateMap() {
        Object res = executeScript("m = {a: 1}; m.a");
        assertTrue(res instanceof Integer);
        assertEquals(1, res);
    }

    public void testCreateEmptyMapAndAssignField() {
        Object res = executeScript("m = {}; m.test = 1; m");
        assertTrue(res instanceof Map);
        assertEquals(1, ((Map) res).size());
        assertEquals(1, ((Map) res).get("test"));
    }

    public void testNonExistentMapField() {
        Object res = executeScript("m = {}; t = m.test; t");
        assertNull(res);
    }

    public void testEqualsOperator() {
        Object res = executeScript("m = 'abc'; m === 'abc'");
        assertTrue(res instanceof Boolean);
        assertTrue((Boolean) res);
        res = executeScript("m = 'abc'; m = 1; m == 1");
        assertTrue(res instanceof Boolean);
        assertTrue((Boolean) res);
    }

    public void testFunctionOrder() {
        Object res = executeScript("function testFunc(m) {m.a +=1;} m = {a: 1}; testFunc(m);   m.a");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("m = {a: 1}; testFunc(m); function testFunc(m) {m.a +=1;}  m.a");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
    }

    public void testComments() {
        Object res = executeScript("//var df = sdfsdf; \n // test comment: comment2 \n m = {\n// c: d, \n /* e: \n\nf, */ a: 2 }; m");
        assertTrue(res instanceof HashMap);
        assertEquals(1, ((Map) res).size());
        assertEquals(2, ((Map) res).get("a"));
    }

    public void testStopExecution() throws Exception {
        AtomicReference<Exception> capturedException = new AtomicReference<>();
        final CountDownLatch countDown = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try {
                executeScript("t = 0; while(true) { t  = 1}; t");
            } catch (Exception e) {
                capturedException.set(e);
            } finally {
                countDown.countDown();
            }
        });
        thread.start();
        boolean result = countDown.await(500, TimeUnit.MILLISECONDS);
        assertFalse(result);
        this.currentExecutionContext.stop();
        result = countDown.await(500, TimeUnit.MILLISECONDS);
        assertTrue(result);
        Exception exception = capturedException.get();
        assertNotNull(exception);
        assertEquals("Script execution is stopped!", exception.getMessage());
    }

    public void testMemoryOverflowVariable() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("t = 'abc'; while(true) { t  += t}; t", new HashMap(), new ExecutionContext(memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowInnerMap1() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = {}; m.put('a', {}); i =0; while(true) { m.get('a').put(i++, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random());}; m", new HashMap(), new ExecutionContext(memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowInnerMap2() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = {}; m.a = {}; i =0; while(true) { m.a[i++] = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random();}; m", new HashMap(), new ExecutionContext(memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowArray() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = []; i =0; while(true) { m.add('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random())}; m", new HashMap(), new ExecutionContext(memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowArrayInnerMap() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = [1]; m[0] = {}; i =0; while(true) { m[0].put(i++, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random())}; m", new HashMap(), new ExecutionContext(memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowAddAll() throws Exception {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("a = ['aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa']; b = []; while(true) { b.addAll(a)}; m", new HashMap(), new ExecutionContext(memoryLimit), 10000);
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testForbidCustomObjects() throws Exception {
        try {
            executeScript("m = new java.util.HashMap(); m");
            fail("Should throw ScriptRuntimeException");
        } catch (ScriptRuntimeException e) {
            assertTrue(e.getMessage().contains("Unsupported value type: class java.util.HashMap"));
        }
    }

    public void testForbiddenClassAccess() {
        try {
            executeScript("m = {5}; System.exit(-1); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: System"));
        }

        try {
            executeScript("m = {5}; exit(-1); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("function not found: exit"));
        }

        try {
            executeScript("m = {5}; java.lang.System.exit(-1); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: java"));
        }

        try {
            executeScript("m = {5}; Runtime.getRuntime().exec(\"echo hi\"); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: Runtime"));
        }

        try {
            executeScript("m = {5}; m.getClass().getClassLoader()");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve method: getClass()"));
        }

        try {
            executeScript("m = {5}; m.class");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not access property: class"));
        }

        Object res = executeScript("m = {class: 5}; m.class");
        assertNotNull(res);
        assertEquals(5, res);
    }

    public void testForbidImport() {
        try {
            executeScript("import java.util.HashMap; m = new HashMap(); m.put('t', 10); m");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Import is forbidden!"));
        }
    }

    public void testUseClassImport() {
        try {
            executeScript("MyTestUtil.getFoo({foo: 'foo-bar'})");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            Assert.assertTrue(e.getMessage().contains("unresolvable property or identifier: MyTestUtil"));
        }
        this.parserConfig.addImport("MyTestUtil", TestUtil.class);
        Object res = executeScript("MyTestUtil.getFoo({foo: 'foo-bar'})");
        assertEquals("foo-bar", res);
        res = executeScript("MyTestUtil.getFoo({})");
        assertEquals("Not found!", res);
        res = executeScript("MyTestUtil.methodWithExecContext('key1', 'val1')");
        assertTrue(res instanceof Map);
        assertEquals("val1", ((Map)res).get("key1"));
        res = executeScript("MyTestUtil.methodWithExecContext2('key2', 'val2')");
        assertTrue(res instanceof Map);
        assertEquals("val2", ((Map)res).get("key2"));
        res = executeScript("MyTestUtil.methodWithExecContext3('key3', 'val3')");
        assertTrue(res instanceof Map);
        assertEquals("val3", ((Map)res).get("key3"));
        res = executeScript("MyTestUtil.methodWithExecContextVarArgs('a1', 'a2', 'a3', 'a4', 'a5')");
        assertTrue(res instanceof List);
        assertEquals(5, ((List)res).size());
        assertArrayEquals(new String[]{"a1", "a2", "a3", "a4", "a5"}, ((List)res).toArray(new String[5]));
    }

    public void testUseStaticMethodImport() throws Exception {
        this.parserConfig.addImport("getFoo", new MethodStub(TestUtil.class.getMethod("getFoo",
                Map.class)));
        Object res = executeScript("getFoo({foo: 'foo-bar'})");
        assertEquals("foo-bar", res);
        res = executeScript("getFoo({})");
        assertEquals("Not found!", res);
        try {
            executeScript("currentTimeMillis()");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            Assert.assertTrue(e.getMessage().contains("function not found: currentTimeMillis"));
        }
        this.parserConfig.addImport("currentTimeMillis", new MethodStub(System.class.getMethod("currentTimeMillis")));
        res = executeScript("currentTimeMillis()");
        assertTrue(res instanceof Long);
        assertEquals(System.currentTimeMillis() / 100, ((long) res) / 100);
        this.parserConfig.addImport("methodWithExecContext", new MethodStub(TestUtil.class.getMethod("methodWithExecContext",
                String.class, Object.class, ExecutionContext.class)));
        res = executeScript("methodWithExecContext('key1', 'val1')");
        assertTrue(res instanceof Map);
        assertEquals("val1", ((Map)res).get("key1"));
        this.parserConfig.addImport("methodWithExecContext2", new MethodStub(TestUtil.class.getMethod("methodWithExecContext2",
                String.class, ExecutionContext.class, Object.class)));
        res = executeScript("methodWithExecContext2('key2', 'val2')");
        assertTrue(res instanceof Map);
        assertEquals("val2", ((Map)res).get("key2"));
        this.parserConfig.addImport("methodWithExecContext3", new MethodStub(TestUtil.class.getMethod("methodWithExecContext3",
                ExecutionContext.class, String.class, Object.class)));
        res = executeScript("methodWithExecContext3('key3', 'val3')");
        assertTrue(res instanceof Map);
        assertEquals("val3", ((Map)res).get("key3"));
        this.parserConfig.addImport("methodWithExecContextVarArgs", new MethodStub(TestUtil.class.getMethod("methodWithExecContextVarArgs",
                ExecutionContext.class, Object[].class)));
        res = executeScript("methodWithExecContextVarArgs('a1', 'a2', 'a3', 'a4', 'a5')");
        assertTrue(res instanceof List);
        assertEquals(5, ((List)res).size());
        assertArrayEquals(new String[]{"a1", "a2", "a3", "a4", "a5"}, ((List)res).toArray(new String[5]));
    }

    private Object executeScript(String ex, Map vars, ExecutionContext executionContext, long timeoutMs) throws Exception {
        final CountDownLatch countDown = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> exception = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                result.set(executeScript(ex, vars, executionContext));
            } catch (Exception e) {
                exception.set(e);
            } finally {
                countDown.countDown();
            }
        });
        thread.start();
        try {
            countDown.await(timeoutMs, TimeUnit.MILLISECONDS);
            executionContext.stop();
            countDown.await(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (exception.get() != null) {
            throw exception.get();
        } else {
            return result.get();
        }
    }

    private Object executeScript(String ex) {
        return executeScript(ex, new HashMap());
    }

    private Object executeScript(String ex, Map vars) {
        return executeScript(ex, vars, new ExecutionContext());
    }

    private Object executeScript(String ex, Map vars, ExecutionContext executionContext) {
        Serializable compiled = compileExpression(ex, new SandboxedParserContext(this.parserConfig));
        this.currentExecutionContext = executionContext;
        return executeTbExpression(compiled, this.currentExecutionContext, vars);
    }

    public static final class TestUtil {
        public static String getFoo(Map input) {
            if (input.containsKey("foo")) {
                return input.get("foo") != null ? input.get("foo").toString() : "null";
            } else {
                return "Not found!";
            }
        }

        public static Map methodWithExecContext(String key, Object val, ExecutionContext ctx) {
            Map map = new ExecutionHashMap(1, ctx);
            map.put(key, val);
            return map;
        }

        public static Map methodWithExecContext2(String key, ExecutionContext ctx, Object val) {
            Map map = new ExecutionHashMap(1, ctx);
            map.put(key, val);
            return map;
        }

        public static Map methodWithExecContext3(ExecutionContext ctx, String key, Object val) {
            Map map = new ExecutionHashMap(1, ctx);
            map.put(key, val);
            return map;
        }

        public static List methodWithExecContextVarArgs(ExecutionContext ctx, Object... vals) {
            List list = new ExecutionArrayList(Arrays.asList(vals), ctx);
            return list;
        }
    }
}
