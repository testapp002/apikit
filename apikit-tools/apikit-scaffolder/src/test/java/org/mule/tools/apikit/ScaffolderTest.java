/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.tools.apikit.Helper.countOccurences;

import org.mule.raml.implv2.ParserV2Utils;
import org.mule.tools.apikit.misc.FileListUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ScaffolderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private FileListUtils fileListUtils = new FileListUtils();

    @Before
    public void setUp() {
        folder.newFolder("scaffolder");
        folder.newFolder("double-root-raml");
        folder.newFolder("custom-domain");
        folder.newFolder("empty-domain");
        folder.newFolder("custom-domain-multiple-lc");
    }

    @Test
    public void testSimpleGenerateV08() throws Exception
    {
        testSimpleGenerate("simple");
    }

    @Test
    public void testSimpleGenerateV10() throws Exception
    {
        testSimpleGenerate("simpleV10");
    }

    public void testSimpleGenerate(String name) throws Exception {
        File muleXmlSimple = simpleGeneration(name, null, "3.8.0");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "get:/:" + name + "-config"));
        assertEquals(1, countOccurences(s, "get:/pet:" + name + "-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
        assertEquals(2, countOccurences(s, "#[NullPayload.getInstance()]"));
        assertEquals(3, countOccurences(s, "exception-strategy"));
        assertEquals(0, countOccurences(s, "error-handler name=\"simpleV10-apiKitGlobalExceptionMapping\""));
        assertEquals(0, countOccurences(s, "expression-component>flowVars['_outboundHeaders_'].put('Content-Type', 'application/json')</expression-component>"));
        assertEquals(0, countOccurences(s, "set-variable variableName=\"_outboundHeaders_\" value=\"#[new java.util.HashMap()]\" />"));


    }

    @Test
    public void testSimpleGenerateWithExtensionWithOldParser() throws Exception
    {
        testSimpleGenerateWithExtension();
    }

    @Test
    public void generateWithIncludes08() throws Exception {
        String filepath = ScaffolderTest.class.getClassLoader().getResource("scaffolder-include-08/api.raml").getFile();
        File file = new File(filepath);
        List<File> ramls = Arrays.asList(file);
        List<File> xmls = Arrays.asList();
        File muleXmlOut = folder.newFolder("mule-xml-out");
        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, "3.7.0", null);
        scaffolder.run();
        File xmlOut = new File (muleXmlOut, "api.xml");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertNotNull(s);
        assertEquals(1, countOccurences(s, "post:/Queue:application/json:api-config"));
        assertEquals(1, countOccurences(s, "post:/Queue:text/xml:api-config"));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void generateWithIncludes10() throws Exception {
        String filepath = ScaffolderTest.class.getClassLoader().getResource("scaffolder-include-10/api.raml").getFile();
        File file = new File(filepath);
        List<File> ramls = Arrays.asList(file);
        List<File> xmls = Arrays.asList();
        File muleXmlOut = folder.newFolder("mule-xml-out");
        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, "3.7.0", null);
        scaffolder.run();
        File xmlOut = new File (muleXmlOut, "api.xml");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertNotNull(s);
        assertEquals(1, countOccurences(s, "post:/Queue:application/json:api-config"));
        assertEquals(1, countOccurences(s, "post:/Queue:text/xml:api-config"));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithExtensionWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithExtension();
    }

    public void testSimpleGenerateWithExtension() throws Exception {
        File muleXmlFolderOut = simpleGenerationWithExtensionEnabled("simple", "simple", null, "3.8.0");
        File xmlOut = new File (muleXmlFolderOut, "simple.xml");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(1, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithExtensionInNullWithOldParser() throws Exception
    {
        testSimpleGenerateWithExtensionInNull();
    }

    @Test
    public void testSimpleGenerateWithExtensionInNullWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithExtensionInNull();
    }

    public void testSimpleGenerateWithExtensionInNull() throws Exception {
        File muleXmlFolderOut = simpleGenerationWithExtensionEnabled("simple", null, null, "3.8.0");
        File xmlOut = new File (muleXmlFolderOut, "simple.xml");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithInboundEndpointWithOldParser() throws Exception
    {
        testSimpleGenerateWithInboundEndpoint();
    }

    @Test
    public void testSimpleGenerateWithInboundEndpointWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithInboundEndpoint();
    }

    public void testSimpleGenerateWithInboundEndpoint() throws Exception {
        File xmlOut = simpleGeneration("simple", null, "3.5.0");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertEquals(0, countOccurences(s, "<http:listener-config"));
        assertEquals(2, countOccurences(s, "<http:inbound"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithListenerWithOldParser() throws Exception
    {
        testSimpleGenerateWithListener();
    }

    @Test
    public void testSimpleGenerateWithListenerWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithListener();
    }

    public void testSimpleGenerateWithListener() throws Exception {
        File xmlOut = simpleGeneration("simple", null, "3.6.0");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(0, countOccurences(s, "<http:inbound"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithListenerAndExtensionWithOldParser() throws Exception
    {
        testSimpleGenerateWithListenerAndExtension();
    }

    @Test
    public void testSimpleGenerateWithListenerAndExtensionWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithListenerAndExtension();
    }

    public void testSimpleGenerateWithListenerAndExtension() throws Exception {
        File muleXmlFolderOut = simpleGenerationWithExtensionEnabled("simple", "simple", null, "3.8.0");
        File xmlOut = new File (muleXmlFolderOut, "simple.xml");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(0, countOccurences(s, "<http:inbound"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(1, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithCustomDomainWithOldParser() throws Exception
    {
        testSimpleGenerateWithCustomDomain();
    }

    @Test
    public void testSimpleGenerateWithCustomDomainWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithCustomDomain();
    }

    public void testSimpleGenerateWithCustomDomain() throws Exception {
        File muleXmlSimple = simpleGeneration("simple", "custom-domain/mule-domain-config.xml","3.7.3");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(0, countOccurences(s, "<http:listener-config"));
        assertEquals(2, countOccurences(s, "config-ref=\"http-lc-0.0.0.0-8081\""));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithCustomDomainAndExtensionWithOldParser() throws Exception
    {
        testSimpleGenerateWithCustomDomainAndExtension();
    }

    @Test
    public void testSimpleGenerateWithCustomDomainAndExtensionWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithCustomDomainAndExtension();
    }

    public void testSimpleGenerateWithCustomDomainAndExtension() throws Exception {
        File muleXmlFolderOut = simpleGenerationWithExtensionEnabled("simple", "simple", "custom-domain/mule-domain-config.xml", "3.8.0");
        File xmlOut = new File (muleXmlFolderOut, "simple.xml");
        assertTrue(xmlOut.exists());
        String s = IOUtils.toString(new FileInputStream(xmlOut));
        assertEquals(0, countOccurences(s, "<http:listener-config"));
        assertEquals(2, countOccurences(s, "config-ref=\"http-lc-0.0.0.0-8081\""));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(1, countOccurences(s, "extensionEnabled"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithCustomDomainWithMultipleLCWithOldParser() throws Exception
    {
        testSimpleGenerateWithCustomDomainWithMultipleLC();
    }

    @Test
    public void testSimpleGenerateWithCustomDomainWithMultipleLCWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithCustomDomainWithMultipleLC();
    }

    public void testSimpleGenerateWithCustomDomainWithMultipleLC() throws Exception {
        File muleXmlSimple = simpleGeneration("simple", "custom-domain-multiple-lc/mule-domain-config.xml", "3.7.3");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(0, countOccurences(s, "<http:listener-config"));
        assertEquals(2, countOccurences(s, "config-ref=\"abcd\""));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testSimpleGenerateWithEmptyDomainWithOldParser() throws Exception
    {
        testSimpleGenerateWithEmptyDomain();
    }

    @Test
    public void testSimpleGenerateWithEmptyDomainWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testSimpleGenerateWithEmptyDomain();
    }

    public void testSimpleGenerateWithEmptyDomain() throws Exception {
        File muleXmlSimple = simpleGeneration("simple", "empty-domain/mule-domain-config.xml", "3.7.3");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(1, countOccurences(s, "<apikit:console"));
        assertEquals(1, countOccurences(s, "consoleEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testTwoResourceGenerateWithOldParser() throws Exception
    {
        testTwoResourceGenerate();
    }

    @Test
    public void testTwoResourceGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testTwoResourceGenerate();
    }

    @Test
    public void testTwoResourceGenerate() throws Exception {
        File muleXmlSimple = simpleGeneration("two", null, "3.7.3");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));

        assertEquals(1, countOccurences(s, "get:/pet:two-config"));
        assertEquals(1, countOccurences(s, "post:/pet:two-config"));

        assertEquals(1, countOccurences(s, "get:/car:two-config"));
        assertEquals(1, countOccurences(s, "post:/car:two-config"));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testNestedGenerateWithOldParser() throws Exception
    {
        testNestedGenerate();
    }

    @Test
    public void testNestedGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testNestedGenerate();
    }

    public void testNestedGenerate() throws Exception {
        File muleXmlSimple = simpleGeneration("nested", null, "3.7.3");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(1, countOccurences(s, "get:/pet:nested-config"));
        assertEquals(1, countOccurences(s, "post:/pet:nested-config"));
        assertEquals(1, countOccurences(s, "get:/pet/owner:nested-config"));
        assertEquals(1, countOccurences(s, "get:/car:nested-config"));
        assertEquals(1, countOccurences(s, "post:/car:nested-config"));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testNoNameGenerateWithOldParser() throws Exception
    {
        testNoNameGenerate();
    }

    @Test
    public void testNoNameGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testNoNameGenerate();
    }

    public void testNoNameGenerate() throws Exception {
        File muleXmlSimple = simpleGeneration("no-name", null, "3.7.3");
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(1, countOccurences(s, "http:listener-config name=\"no-name-httpListenerConfig\" host=\"0.0.0.0\" port=\"8081\""));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"no-name-httpListenerConfig\" path=\"/api/*\""));
    }

    @Test
    public void testExampleGenerateWithOldParser() throws Exception
    {
        testExampleGenerate();
    }

    @Test
    public void testExampleGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testExampleGenerate();
    }

    public void testExampleGenerate() throws Exception {
        File muleXmlSimple = simpleGeneration("example", null, null);
        assertTrue(muleXmlSimple.exists());
        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));

        assertEquals(1, countOccurences(s, "{&#xA;    &quot;name&quot;: &quot;Bobby&quot;,&#xA;    &quot;food&quot;: &quot;Ice Cream&quot;&#xA;}"));
    }

    @Test
    public void doubleRootRamlWithOldParser() throws Exception {
        doubleRootRaml();
    }

    @Test
    public void doubleRootRamlWithNewParser() throws Exception {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        doubleRootRaml();
    }

    public void doubleRootRaml() throws Exception
    {
        List<File> ramls = Arrays.asList(getFile("double-root-raml/simple.raml"), getFile("double-root-raml/two.raml"));

        List<File> xmls = Arrays.asList();
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, "3.7.3", null);
        scaffolder.run();

        File muleXmlSimple = new File(muleXmlOut, "simple.xml");
        File muleXmlTwo = new File(muleXmlOut, "two.xml");

        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "get:/:simple-config"));
        assertEquals(1, countOccurences(s, "get:/pet:simple-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));

        String s2 = IOUtils.toString(new FileInputStream(muleXmlTwo));
        assertEquals(1, countOccurences(s2, "get:/pet:two-config"));
        assertEquals(1, countOccurences(s2, "post:/pet:two-config"));
        assertEquals(1, countOccurences(s2, "get:/car:two-config"));
        assertEquals(1, countOccurences(s2, "post:/car:two-config"));
        assertEquals(0, countOccurences(s2, "extensionEnabled"));
    }

    private Scaffolder createScaffolder(List<File> ramls, List<File> xmls, File muleXmlOut, File domainFile, String muleVersion, Set<File> ramlsWithExtensionEnabled)
            throws FileNotFoundException {
        Log log = mock(Log.class);
        Map<File, InputStream> ramlMap = null;
        if (ramls != null)
        {
            ramlMap = getFileInputStreamMap(ramls);
        }
        Map<File, InputStream> xmlMap = getFileInputStreamMap(xmls);
        InputStream domainStream = null;
        if (domainFile != null)
        {
            domainStream = new FileInputStream(domainFile);
        }
        return new Scaffolder(log, muleXmlOut, ramlMap, xmlMap, domainStream, muleVersion, ramlsWithExtensionEnabled);
    }

    private Map<File, InputStream> getFileInputStreamMap(List<File> ramls) {
        return fileListUtils.toStreamFromFiles(ramls);
    }

    private File getFile(String s) throws  Exception {
        if (s == null)
        {
            return null;
        }
        File file = folder.newFile(s);
        file.createNewFile();
        InputStream resourceAsStream = ScaffolderTest.class.getClassLoader().getResourceAsStream(s);
        IOUtils.copy(resourceAsStream,
                     new FileOutputStream(file));
        return file;
    }

    private File simpleGeneration(String name, String domainPath, String muleVersion) throws Exception
    {
        return simpleGeneration("scaffolder", name, domainPath, muleVersion);
    }

    private File simpleGeneration(String apiPath, String name, String domainPath, String muleVersion) throws Exception {
        List<File> ramls = Arrays.asList(getFile(apiPath + "/" + name + ".raml"));
        File domainFile = getFile(domainPath);

        List<File> xmls = Arrays.asList();
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, domainFile, muleVersion, null);
        scaffolder.run();

        return new File(muleXmlOut, name + ".xml");
    }

    private File simpleGenerationWithExtensionEnabled(String raml, String ramlWithExtensionEnabledPath, String domainPath, String muleVersion) throws Exception
    {
        List<File> ramlList = Arrays.asList(getFile("scaffolder/" + raml + ".raml"));
        Set<File> ramlWithExtensionEnabled = null;
        if (ramlWithExtensionEnabledPath != null)
        {
            ramlWithExtensionEnabled = new TreeSet<>();
            ramlWithExtensionEnabled.add(getFile("scaffolder/" + ramlWithExtensionEnabledPath + ".raml"));
        }
        File domainFile = getFile(domainPath);

        List<File> xmls = Arrays.asList();
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramlList, xmls, muleXmlOut, domainFile, muleVersion, ramlWithExtensionEnabled);
        scaffolder.run();

        return muleXmlOut;
    }

    @After
    public void after()
    {
        System.clearProperty(ParserV2Utils.PARSER_V2_PROPERTY);
    }
}
