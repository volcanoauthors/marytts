package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.FileUtils;

/**
 * For the given texts, compute unit features and align them
 * with the given unit labels.
 * @author schroed
 *
 */
public class UnitFeatureComputer implements VoiceImportComponent
{
    protected File textDir;
    protected File unitfeatureDir;
    protected String locale;
    protected MaryClient mary;
    
    public static String getMaryXMLHeaderWithInitialBoundary(String locale)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<maryxml version=\"0.4\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
            "xml:lang=\"" + locale + "\">\n" +
            "<boundary duration=\"100\"/>\n";
        
    }
    
    public UnitFeatureComputer()
    throws IOException
    {
        textDir = new File(System.getProperty("text.dir", "text"));
        if (!textDir.exists()) throw new IOException("No such directory: "+ textDir);
        unitfeatureDir = new File(System.getProperty("unitfeatures.dir", "unitfeatures"));
        if (!unitfeatureDir.exists()) unitfeatureDir.mkdir();
        locale = System.getProperty("locale", "en");

        mary = null; // initialised only if needed
    }
    
    public File getTextDir()
    {
        return textDir;
    }
    
    public File getUnitFeatureDir()
    {
        return unitfeatureDir;
    }
    
    public String getLocale()
    {
        return locale;
    }
    
    public MaryClient getMaryClient() throws IOException
    {
        if (mary == null) {
            if (System.getProperty("server.host") == null) {
                System.setProperty("server.host", "localhost");
            }
            if (System.getProperty("server.port") == null) {
                System.setProperty("server.port", "59125");
            }
            mary = new MaryClient();
        }
        return mary;
    }

    public boolean compute() throws IOException
    {
        String[] basenames = FileUtils.listBasenames(textDir, ".txt");
        System.out.println("Computing unit features for "+basenames.length+" files");
        for (int i=0; i<basenames.length; i++) {
            computeFeaturesFor(basenames[i]);
            System.out.println("    "+basenames[i]);
        }
        System.out.println("Finished computing unit features");
        return true;
    }

    public void computeFeaturesFor(String basename) throws IOException
    {
        String text;
        // First, test if there is a corresponding .rawmaryxml file in textdir:
        File rawmaryxmlFile = new File(textDir, basename+".rawmaryxml");
        if (rawmaryxmlFile.exists()) {
            text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
        } else {
            text = getMaryXMLHeaderWithInitialBoundary(locale)
                + FileUtils.getFileAsString(new File(textDir, basename+".txt"), "UTF-8")
                + "</maryxml>";
        }
        String inputFormat = "RAWMARYXML";
        String outputFormat = "TARGETFEATURES";
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(unitfeatureDir, basename+".feats")));
        MaryClient maryClient = getMaryClient();
        maryClient.process(text, inputFormat, outputFormat, null, null, os);
        os.flush();
        os.close();
    }

    public static void main(String[] args) throws IOException
    {
        new UnitFeatureComputer().compute();
    }
}
