package org.mvel2.tests.core;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.SandboxedParserContext;
import org.mvel2.ScriptMemoryOverflowException;
import org.mvel2.ScriptRuntimeException;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeExpression;

public class TbExpressionsTest extends TestCase {

    private SandboxedParserContext parserContext;

    private ExecutionContext currentExecutionContext;

    @Override
    protected void setUp() throws Exception {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        super.setUp();
        this.parserContext = new SandboxedParserContext();
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

    public void testUseClassImport() {
        try {
            executeScript("MyTestUtil.getFoo({foo: 'foo-bar'})");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            Assert.assertTrue(e.getMessage().contains("unresolvable property or identifier: MyTestUtil"));
        }
        this.parserContext.addImport("MyTestUtil", TestUtil.class);
        Object res = executeScript("MyTestUtil.getFoo({foo: 'foo-bar'})");
        assertEquals("foo-bar", res);
        res = executeScript("MyTestUtil.getFoo({})");
        assertEquals("Not found!", res);
    }

    public void testUseStaticMethodImport() throws Exception {
        this.parserContext.addImport("getFoo", new MethodStub(TestUtil.class.getMethod("getFoo",
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
        this.parserContext.addImport("currentTimeMillis", new MethodStub(System.class.getMethod("currentTimeMillis")));
        res = executeScript("currentTimeMillis()");
        assertTrue(res instanceof Long);
        assertEquals(System.currentTimeMillis() / 100, ((long) res) / 100);
    }

    public void testJsonStringify() throws Exception {
        Object res = executeScript("m = {foo: 'bar', a: 1, b: true}; JSON.stringify(m)");
        assertEquals("{\"a\":1,\"b\":true,\"foo\":\"bar\"}", res);
    }

    public void testJsonParse() throws Exception {
        Object res = executeScript("str = '{\"foo\": \"bar\", \"a\": 1, \"b\": true}'; JSON.parse(str)");
        assertTrue(res instanceof Map);
        assertEquals(3, ((Map)res).size());
        assertEquals("bar", ((Map)res).get("foo"));
        assertEquals(1, ((Map)res).get("a"));
        assertEquals(true, ((Map)res).get("b"));
        res = executeScript("str = '{\"foo\": \"bar\", \"a\": 1, \"b\": true}'; function parseStr(a) { return JSON.parse(a); }; m = parseStr(str); m");
        assertTrue(res instanceof Map);
        assertEquals(3, ((Map)res).size());
        assertEquals("bar", ((Map)res).get("foo"));
        assertEquals(1, ((Map)res).get("a"));
        assertEquals(true, ((Map)res).get("b"));
    }

    public void testMemoryOverflowJsonParse() throws Exception {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("str = '{\"foo\": \"bar\", \"a\": 1, \"b\": true, \"foo2\": \"bar2\", \"a2\": 2, \"b2\": false}'; while(true) {JSON.parse(str)};",
                    new HashMap(), new ExecutionContext(memoryLimit), 10000);
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
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
        Serializable compiled = compileExpression(ex, parserContext);
        this.currentExecutionContext = executionContext;
        return executeExpression(compiled, this.currentExecutionContext, vars);
    }

    public static final class TestUtil {
        public static String getFoo(Map input) {
            if (input.containsKey("foo")) {
                return input.get("foo") != null ? input.get("foo").toString() : "null";
            } else {
                return "Not found!";
            }
        }
    }
}
