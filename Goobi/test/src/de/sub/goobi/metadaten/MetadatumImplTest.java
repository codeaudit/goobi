package de.sub.goobi.metadaten;

import static org.junit.Assert.*;

import java.util.List;

import javax.faces.model.SelectItem;

import org.goobi.api.display.Item;
import org.goobi.beans.Process;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import de.sub.goobi.mock.MockProcess;

public class MetadatumImplTest {

    private Prefs prefs;
    private Process process;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        process = MockProcess.createProcess(folder);
        prefs = process.getRegelsatz().getPreferences();
    }

    @Test
    public void testMetadatumImpl() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        assertNotNull(md);
    }

    @Test
    public void testWert() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        String value = "test";
        md.setWert(value);
        assertEquals(value, m.getValue());
    }

    @Test
    public void testTyp() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        md.setTyp("junitMetadata");
        assertEquals("junitMetadata", md.getTyp());
    }

    @Test
    public void testGetIdentifier() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        md.setIdentifier(1);
        assertEquals(1, md.getIdentifier());
    }

    @Test
    public void testtMd() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);

        md.setMd(m);
        assertEquals(m, md.getMd());

    }

    @Test
    public void testGetOutputType() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        assertEquals("textarea", md.getOutputType());
    }

    @Test
    public void testGetItems() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        List<SelectItem> items = md.getItems();
        assertNotNull(items);

    }

    @Test
    public void testGetSelectedItems() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        List<String> items = md.getSelectedItems();
        assertNotNull(items);
    }

    @Test
    public void testGetSelectedItem() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        String item = md.getSelectedItem();
        assertNotNull(item);
    }

    @Test
    public void testGetValue() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);

        md.setValue("value");
        assertEquals("value", md.getValue());

    }

    @Test
    public void testGetPossibleDatabases() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        List<String> databases = md.getPossibleDatabases();
        assertNotNull(databases);
    }

    @Test
    public void testGetNormdataValue() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        String id = md.getNormdataValue();
        assertNull(id);
    }

    @Test
    public void testSetNormdataValue() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        String id = md.getNormdataValue();
        assertNull(id);
        md.setNormdataValue("value");
        assertEquals("value", md.getNormdataValue());
        
    }

    @Test
    public void testNormDatabase() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        List<String> databases = md.getPossibleDatabases();
        md.setNormDatabase(databases.get(0));
        assertEquals("gnd", md.getNormDatabase());
    }

   
    @Test
    public void testIsNormdata() throws MetadataTypeNotAllowedException {
        Metadata m = new Metadata(prefs.getMetadataTypeByName("junitMetadata"));
        MetadatumImpl md = new MetadatumImpl(m, 0, prefs, process);
        assertFalse(md.isNormdata());
    }

}