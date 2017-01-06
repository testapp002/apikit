package org.mule.tools.apikit;

import static org.junit.Assert.assertEquals;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ScaffolderWithExistingConfig
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private FileListUtils fileListUtils = new FileListUtils();

    @Before
    public void setUp() {
        folder.newFolder("scaffolder");
        folder.newFolder("scaffolder-existing");
        folder.newFolder("scaffolder-existing-extension");
        folder.newFolder("scaffolder-existing-custom-lc");
        folder.newFolder("scaffolder-existing-old");
        folder.newFolder("scaffolder-existing-old-address");
        folder.newFolder("scaffolder-existing-custom-and-normal-lc");
        folder.newFolder("custom-domain");
        folder.newFolder("empty-domain");
        folder.newFolder("custom-domain-multiple-lc");
    }

    @Test
    public void testAlreadyExistsWithExtensionDisabledWithOldParser() throws Exception
    {
        testAlreadyExistsWithExtensionDisabled();
    }

    @Test
    public void testAlreadyExistsWithExtensionDisabledWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsWithExtensionDisabled();
    }

    public void testAlreadyExistsWithExtensionDisabled() throws Exception
    {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-extension/simple.raml"));
        File xmlFile = getFile("scaffolder-existing-extension/simple-extension-disabled.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Set<File> ramlwithEE = new TreeSet<>();
        ramlwithEE.add(getFile("scaffolder-existing-extension/simple.raml"));
        createScaffolder(ramls, xmls, muleXmlOut, null, "3.7.3", ramlwithEE).run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(1, countOccurences(s, "http:listener-config name=\"HTTP_Listener_Configuration\" host=\"localhost\" port=\"${serverPort}\""));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"HTTP_Listener_Configuration\" path=\"/api/*\""));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "post:/pet"));
        assertEquals(1, countOccurences(s, "get:/\""));
        assertEquals(1, countOccurences(s, "extensionEnabled=\"false\""));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testAlreadyExistsWithExtensionEnabledWithOldParser() throws Exception
    {
        testAlreadyExistsWithExtensionEnabled();
    }

    @Test
    public void testAlreadyExistsWithExtensionEnabledWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsWithExtensionEnabled();
    }

    @Test
    public void testAlreadyExistsWithExtensionEnabled() throws Exception
    {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-extension/simple.raml"));
        File xmlFile = getFile("scaffolder-existing-extension/simple-extension-enabled.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Set<File> ramlwithEE = new TreeSet<>();
        ramlwithEE.add(getFile("scaffolder-existing-extension/simple.raml"));
        createScaffolder(ramls, xmls, muleXmlOut, null, "3.7.3", ramlwithEE).run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(1, countOccurences(s, "http:listener-config name=\"HTTP_Listener_Configuration\" host=\"localhost\" port=\"${serverPort}\""));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"HTTP_Listener_Configuration\" path=\"/api/*\""));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "post:/pet"));
        assertEquals(1, countOccurences(s, "get:/\""));
        assertEquals(1, countOccurences(s, "extensionEnabled=\"true\""));
    }

    @Test
    public void testAlreadyExistsWithExtensionNotPresentWithOldParser() throws Exception
    {
        testAlreadyExistsWithExtensionNotPresent();
    }

    @Test
    public void testAlreadyExistsWithExtensionNotPresentWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsWithExtensionNotPresent();
    }

    public void testAlreadyExistsWithExtensionNotPresent() throws Exception
    {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-extension/simple.raml"));
        File xmlFile = getFile("scaffolder-existing-extension/simple-extension-not-present.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Set<File> ramlwithEE = new TreeSet<>();
        ramlwithEE.add(getFile("scaffolder-existing-extension/simple.raml"));
        createScaffolder(ramls, xmls, muleXmlOut, null, "3.7.3", ramlwithEE).run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(1, countOccurences(s, "http:listener-config name=\"HTTP_Listener_Configuration\" host=\"localhost\" port=\"${serverPort}\""));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"HTTP_Listener_Configuration\" path=\"/api/*\""));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "post:/pet"));
        assertEquals(1, countOccurences(s, "get:/\""));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testAlreadyExistsGenerateWithOldParser() throws Exception
    {
        testAlreadyExistsGenerate();
    }

    @Test
    public void testAlreadyExistsGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsGenerate();
    }

    public void testAlreadyExistsGenerate() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing/simple.raml"));
        File xmlFile = getFile("scaffolder-existing/simple.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, null, null);
        scaffolder.run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(1, countOccurences(s, "http:listener-config name=\"HTTP_Listener_Configuration\" host=\"localhost\" port=\"${serverPort}\""));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"HTTP_Listener_Configuration\" path=\"/api/*\""));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "post:/pet"));
        assertEquals(1, countOccurences(s, "get:/\""));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
        assertEquals(0, countOccurences(s, "#[null]"));
    }

    @Test
    public void testAlreadyExistsGenerateWithCustomDomainWithOldParser() throws Exception
    {
        testAlreadyExistsGenerateWithCustomDomain();
    }

    @Test
    public void testAlreadyExistsGenerateWithCustomDomainWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsGenerateWithCustomDomain();
    }

    public void testAlreadyExistsGenerateWithCustomDomain() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-custom-lc/simple.raml"));
        File xmlFile = getFile("scaffolder-existing-custom-lc/simple.xml");
        File domainFile = getFile("custom-domain/mule-domain-config.xml");

        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");
        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut,domainFile, null, null);
        scaffolder.run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(0, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"http-lc-0.0.0.0-8081\" path=\"/api/*\""));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "get:/\""));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testAlreadyExistsGenerateWithCustomAndNormalLCWithOldParser() throws Exception
    {
        testAlreadyExistsGenerateWithCustomAndNormalLC();
    }

    @Test
    public void testAlreadyExistsGenerateWithCustomAndNormalLCWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsGenerateWithCustomAndNormalLC();
    }

    public void testAlreadyExistsGenerateWithCustomAndNormalLC() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-custom-and-normal-lc/leagues-custom-normal-lc.raml"));
        File xmlFile = getFile("scaffolder-existing-custom-and-normal-lc/leagues-custom-normal-lc.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");
        File domainFile = getFile("custom-domain/mule-domain-config.xml");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, domainFile, null, null);
        scaffolder.run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(1, countOccurences(s, "<http:listener-config"));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"http-lc-0.0.0.0-8081\" path=\"/api/*\""));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/leagues/{leagueId}"));
        assertEquals(1, countOccurences(s, "<http:listener config-ref=\"HTTP_Listener_Configuration\""));
        assertEquals(1, countOccurences(s, "<http:listener config-ref=\"http-lc-0.0.0.0-8081\""));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testAlreadyExistsOldGenerateWithOldParser() throws Exception
    {
        testAlreadyExistsOldGenerate();
    }

    @Test
    public void testAlreadyExistsOldGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsOldGenerate();
    }

    public void testAlreadyExistsOldGenerate() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-old/simple.raml"));
        File xmlFile = getFile("scaffolder-existing-old/simple.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, null, null);
        scaffolder.run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(0, countOccurences(s, "http:listener-config"));
        assertEquals(0, countOccurences(s, "http:listener"));
        assertEquals(1, countOccurences(s, "http:inbound-endpoint port=\"${serverPort}\" host=\"localhost\" path=\"api\""));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "post:/pet"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testAlreadyExistingMuleConfigWithApikitRouterWithOldParser() throws Exception
    {
        testAlreadyExistingMuleConfigWithApikitRouter();
    }

    @Test
    public void testAlreadyExistingMuleConfigWithApikitRouterWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistingMuleConfigWithApikitRouter();
    }

    public void testAlreadyExistingMuleConfigWithApikitRouter() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing/simple.raml"));
        File xmlFile = getFile("scaffolder-existing/mule-config-no-api-flows.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, null, null);
        scaffolder.run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(1, countOccurences(s, "http:listener-config name=\"HTTP_Listener_Configuration\" host=\"localhost\" port=\"${serverPort}\""));
        assertEquals(1, countOccurences(s, "http:listener config-ref=\"HTTP_Listener_Configuration\" path=\"/api/*\""));
        assertEquals(1, countOccurences(s, "<apikit:router config-ref=\"apikit-config\" />"));
        assertEquals(0, countOccurences(s, "http:inbound-endpoint"));
        assertEquals(1, countOccurences(s, "get:/pet"));
        assertEquals(1, countOccurences(s, "post:/pet"));
        assertEquals(1, countOccurences(s, "get:/:"));
        Collection<File> newXmlConfigs = FileUtils.listFiles(muleXmlOut, new String[] {"xml"}, true);
        assertEquals(0, newXmlConfigs.size());
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testAlreadyExistsOldWithAddressGenerateWithOldParser() throws Exception
    {
        testAlreadyExistsOldWithAddressGenerate();
    }

    @Test
    public void testAlreadyExistsOldWithAddressGenerateWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testAlreadyExistsOldWithAddressGenerate();
    }

    @Test
    public void testAlreadyExistsOldWithAddressGenerate() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder-existing-old-address/complex.raml"));
        File xmlFile = getFile("scaffolder-existing-old-address/complex.xml");
        List<File> xmls = Arrays.asList(xmlFile);
        File muleXmlOut = folder.newFolder("mule-xml-out");

        Scaffolder scaffolder = createScaffolder(ramls, xmls, muleXmlOut, null, null, null);
        scaffolder.run();

        assertTrue(xmlFile.exists());
        String s = IOUtils.toString(new FileInputStream(xmlFile));
        assertEquals(0, countOccurences(s, "http:listener-config"));
        assertEquals(0, countOccurences(s, "http:listener"));
        assertEquals(1, countOccurences(s, "http:inbound-endpoint address"));
        assertEquals(1, countOccurences(s, "put:/clients/{clientId}:complex-config"));
        assertEquals(1, countOccurences(s, "put:/invoices/{invoiceId}:complex-config"));
        assertEquals(1, countOccurences(s, "put:/items/{itemId}:application/json:complex-config"));
        assertEquals(1, countOccurences(s, "put:/providers/{providerId}:complex-config"));
        assertEquals(1, countOccurences(s, "delete:/clients/{clientId}:complex-config"));
        assertEquals(1, countOccurences(s, "delete:/invoices/{invoiceId}:complex-config"));
        assertEquals(1, countOccurences(s, "delete:/items/{itemId}:multipart/form-data:complex-config"));
        assertEquals(1, countOccurences(s, "delete:/providers/{providerId}:complex-config"));
        assertEquals(1, countOccurences(s, "get:/:complex-config"));
        assertEquals(1, countOccurences(s, "get:/clients/{clientId}:complex-config"));
        assertEquals(1, countOccurences(s, "get:/clients:complex-config"));
        assertEquals(1, countOccurences(s, "get:/invoices/{invoiceId}:complex-config"));
        assertEquals(1, countOccurences(s, "get:/invoices:complex-config"));
        assertEquals(1, countOccurences(s, "get:/items/{itemId}:complex-config"));
        assertEquals(1, countOccurences(s, "get:/items:complex-config"));
        assertEquals(1, countOccurences(s, "get:/providers/{providerId}:complex-config"));
        assertEquals(1, countOccurences(s, "get:/providers:complex-config"));
        assertEquals(1, countOccurences(s, "post:/clients:complex-config"));
        assertEquals(1, countOccurences(s, "post:/invoices:complex-config"));
        assertEquals(1, countOccurences(s, "post:/items:application/json:complex-config"));
        assertEquals(1, countOccurences(s, "post:/providers:complex-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testMultipleMimeTypesWithoutNamedConfigWithOldParser() throws Exception
    {
        testMultipleMimeTypesWithoutNamedConfig();
    }

    @Test
    public void testMultipleMimeTypesWithoutNamedConfigWithNewParser() throws Exception
    {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testMultipleMimeTypesWithoutNamedConfig();
    }

    public void testMultipleMimeTypesWithoutNamedConfig() throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder/multipleMimeTypes.raml"));
        File muleXmlOut = folder.newFolder("scaffolder");
        List<File> xmls = Arrays.asList(getFile("scaffolder/multipleMimeTypes.xml"));

        createScaffolder(ramls, xmls, muleXmlOut, null, null, null).run();

        File muleXmlSimple = new File(muleXmlOut, "multipleMimeTypes.xml");
        assertTrue(muleXmlSimple.exists());

        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertTrue(s.contains("post:/pet:application/json"));
        assertTrue(s.contains("post:/pet:text/xml"));
        assertTrue(s.contains("post:/pet:application/x-www-form-urlencoded"));
        assertTrue(s.contains("post:/pet"));
        assertTrue(!s.contains("post:/pet:application/xml"));
        assertTrue(s.contains("post:/vet"));
        assertTrue(!s.contains("post:/vet:application/xml"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
    }

    @Test
    public void testMultipleMimeTypesWithOldParser() throws Exception {
        testMultipleMimeTypes("multipleMimeTypes");
    }

    @Test
    public void testMultipleMimeTypesWithNewParser() throws Exception {
        System.setProperty(ParserV2Utils.PARSER_V2_PROPERTY, "true");
        testMultipleMimeTypes("multipleMimeTypes");
    }

    @Test
    public void testMultipleMimeTypesV10() throws Exception {
        testMultipleMimeTypes("multipleMimeTypesV10");
    }

    private void testMultipleMimeTypes(String name) throws Exception {
        List<File> ramls = Arrays.asList(getFile("scaffolder/" + name + ".raml"));
        File muleXmlOut = folder.newFolder("scaffolder");

        createScaffolder(ramls, new ArrayList<File>(), muleXmlOut, null, null, null).run();

        File muleXmlSimple = new File(muleXmlOut, name + ".xml");
        assertTrue(muleXmlSimple.exists());

        String s = IOUtils.toString(new FileInputStream(muleXmlSimple));
        assertTrue(s.contains("post:/pet:application/json:" + name + "-config"));
        assertTrue(s.contains("post:/pet:text/xml:" + name + "-config"));
        if (name.endsWith("V10"))
        {
            assertTrue(s.contains("post:/pet:" + name + "-config"));
        }
        else
        {
            assertTrue(s.contains("post:/pet:application/x-www-form-urlencoded:" + name + "-config"));
        }
        assertTrue(s.contains("post:/pet:" + name + "-config"));
        assertTrue(!s.contains("post:/pet:application/xml:" + name + "-config"));
        assertTrue(s.contains("post:/vet:" + name + "-config"));
        assertTrue(!s.contains("post:/vet:application/xml:" + name + "-config"));
        assertEquals(0, countOccurences(s, "extensionEnabled"));
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

    private Scaffolder createScaffolder(List<File> ramls, List<File> xmls, File muleXmlOut, File domainFile, String muleVersion, Set<File> ramlsWithExtensionEnabled)
            throws FileNotFoundException
    {
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

    @After
    public void after()
    {
        System.clearProperty(ParserV2Utils.PARSER_V2_PROPERTY);
    }
}
