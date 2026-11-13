package com.liuzhihang.doc.view.integration.mcp;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import junit.framework.TestCase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;

public class DocViewMcpProjectScopeTest extends TestCase {
    private Path dir;
    @Override protected void setUp() throws Exception { dir=Files.createTempDirectory("doc-view-mcp-scope"); }
    @Override protected void tearDown() throws Exception { Files.deleteIfExists(dir.resolve("a"));Files.deleteIfExists(dir.resolve("b"));Files.deleteIfExists(dir); }
    private Project project(String name) throws Exception {
        return project(name, false);
    }
    private Project project(String name, boolean disposed) throws Exception {
        String path=Files.createDirectories(dir.resolve(name)).toRealPath().toString();
        return (Project)Proxy.newProxyInstance(Project.class.getClassLoader(),new Class<?>[]{Project.class},(p,m,a)->switch(m.getName()){
            case "getBasePath"->path; case "isDisposed"->disposed; case "toString"->name; case "hashCode"->System.identityHashCode(p); case "equals"->p==a[0]; default->null;
        });
    }
    public void testExplicitBindingNeverFallsBackAcrossProjects() throws Exception {
        Project a=project("a"), b=project("b"); JsonObject args=new JsonObject();args.addProperty("projectPath",a.getBasePath());
        assertSame(a,DocViewMcpProjectScope.resolve(args,null,new Project[]{a,b}).project());
        try {DocViewMcpProjectScope.resolve(args,b,new Project[]{a,b});fail("Cross-project context must reject");} catch(IllegalArgumentException expected){assertEquals("PROJECT_NOT_OPEN",expected.getMessage());}
        try {DocViewMcpProjectScope.resolve(args,null,new Project[]{b});fail("Closed A must not route to B");} catch(IllegalArgumentException expected){assertEquals("PROJECT_NOT_OPEN",expected.getMessage());}
    }
    public void testHostBindingFillsRemovedReservedParameter() throws Exception {
        Project a=project("a"); var normalized=DocViewMcpProjectScope.resolve(new JsonObject(),a,new Project[]{a});
        assertEquals(a.getBasePath(),normalized.arguments().get("projectPath").getAsString());
        try {DocViewMcpProjectScope.resolve(new JsonObject(),null,new Project[]{a});fail("No implicit first project");} catch(IllegalArgumentException expected){assertEquals("INVALID_ARGUMENT",expected.getMessage());}
    }
    public void testMissingUnrelatedProjectDirectoryDoesNotBlockValidProject() throws Exception {
        Project a=project("a"), b=project("b"); Files.delete(dir.resolve("b"));
        JsonObject args=new JsonObject();args.addProperty("projectPath",a.getBasePath());
        assertSame(a,DocViewMcpProjectScope.resolve(args,null,new Project[]{b,a}).project());
    }
    public void testAmbiguousAndDisposedProjectsAreRejected() throws Exception {
        Project first=project("a"), alias=project("a"), closed=project("b",true);
        JsonObject args=new JsonObject();args.addProperty("projectPath",first.getBasePath());
        try {DocViewMcpProjectScope.resolve(args,null,new Project[]{first,alias});fail("Ambiguous projects must reject");}
        catch(IllegalArgumentException expected){assertEquals("PROJECT_NOT_OPEN",expected.getMessage());}
        args.addProperty("projectPath",closed.getBasePath());
        try {DocViewMcpProjectScope.resolve(args,null,new Project[]{first,closed});fail("Disposed projects must reject");}
        catch(IllegalArgumentException expected){assertEquals("PROJECT_NOT_OPEN",expected.getMessage());}
    }
    public void testInvalidRawPathsAreNotSilentlyNormalizedBeforeValidation() throws Exception {
        Project a=project("a");
        for(String path:new String[]{a.getBasePath()+"/../a", a.getBasePath()+"\0", "relative", ""}) {
            JsonObject args=new JsonObject();args.addProperty("projectPath",path);
            try {DocViewMcpProjectScope.resolve(args,null,new Project[]{a});fail("Invalid path must reject");}
            catch(IllegalArgumentException expected){assertEquals("INVALID_ARGUMENT",expected.getMessage());}
        }
    }
}
