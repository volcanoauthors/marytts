/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.tools.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

/**
 * This class aligns a label file with an XML file in MARY ALLOPHONES format,
 * modifying the structure of the XML file as needed to match the label file.
 * After calling alignXMLTranscriptions(), it is guaranteed that
 * an iteration through all PHONE and BOUNDARY nodes of the XML file matches the label file.
 * @author marc
 *
 */
public class TranscriptionAligner 
{

    protected Map<String, Integer> aligncost;
    protected int defaultcost;
    protected int defaultBoundaryCost;
    protected int skipcost;
    protected AllophoneSet allophoneSet;
    // String for a boundary
    protected String possibleBnd;
    protected String entrySeparator;
    
    protected boolean ensureInitialBoundary = false;

    public TranscriptionAligner()
    {
        this(null);
    }
    
    public TranscriptionAligner(AllophoneSet allophoneSet)
    {
        this(allophoneSet, null);
    }

    public TranscriptionAligner(AllophoneSet allophoneSet, String entrySeparator)
    {
        this.aligncost = new HashMap<String, Integer>();
        this.defaultcost = 10;
    
        // phone set is used for splitting the sampa strings and setting the costs
        this.allophoneSet = allophoneSet;
        if (allophoneSet != null) {
            possibleBnd = allophoneSet.getSilence().name();
        } else {
            possibleBnd = "_";
        }
        if (entrySeparator != null) {
            this.entrySeparator = entrySeparator;
        } else {
            this.entrySeparator = "|";
        }
        
        this.setDistance();
        
        defaultcost = this.getMaxCost();
        // align boundaries only to itself
        defaultBoundaryCost = 20 * defaultcost;
        // distance between pauses is zero, with slight conservative bias
        aligncost.put(possibleBnd + " " + possibleBnd, 0);
        
        skipcost = defaultcost * 1 / 10; // 0.25 / 0.3 /0.33 seem all fine
    }

    public void SetEnsureInitialBoundary(boolean value)
    {
        this.ensureInitialBoundary = value;
    }
    
    public boolean getEnsureInitialBoundary()
    {
        return ensureInitialBoundary;
    }
    
    public String getEntrySeparator()
    {
        return entrySeparator;
    }
    
    
    

    /**
     * This reads in a label file and returns a String of the phonetic symbols,
     * separated by the entry separator character entrySeparator.
     * 
     * @throws IOException if something goes wrong with opening/reading the file
     * 
     */
    public String readLabelFile(String trfname) throws IOException
    {
        // reader for label file.
        BufferedReader lab = new BufferedReader(new FileReader(trfname));
        try {
            StringBuilder result = new StringBuilder();
            
            String line;
            
            while ((line = lab.readLine()) != null){
                if ( line.startsWith("#") ) 
                    continue;
                
                String[] lineLmnts = line.split("\\s+");
                
               if ( lineLmnts.length != 3 )
                   throw new IllegalArgumentException("Expected three columns in label file, got " + lineLmnts.length);
               
               if (result.length() > 0) result.append(entrySeparator);
               result.append(lineLmnts[2]);
            }
            
            // if Label File does not start with pause symbol, insert it
            // as well as a pause duration of zero (...)
            if(ensureInitialBoundary && result.charAt(0) != '_') {
                result.insert(0, "_"+entrySeparator);
            }
            return result.toString();
        } finally {
            lab.close();
        }
    }

    /**
     * This sets the distance by using the phone set of the aligner object.
     * Phoneme set must already be specified.
     */
    private void setDistance()
    {
        
        if (null == this.allophoneSet ) {
            System.err.println("No allophone set -- cannot use intelligent distance metrics");
            return;
        }
        
        for (String fromSym : this.allophoneSet.getAllophoneNames()) {
            for (String toSym : this.allophoneSet.getAllophoneNames()) {
                
                int diff = 0;
                
                Allophone fromPh = this.allophoneSet.getAllophone(fromSym);
                Allophone toPh = this.allophoneSet.getAllophone(toSym);
                
                // for each difference increase distance
                diff += (!fromSym.equals(toSym))? 2:0;
                diff += (fromPh.isFricative() != toPh.isFricative())? 2:0;
                diff += (fromPh.isGlide() != toPh.isGlide())?   2:0;
                diff += (fromPh.isLiquid() != toPh.isLiquid())? 2:0;
                diff += (fromPh.isNasal() != toPh.isNasal())?   2:0;
                diff += (fromPh.isPlosive() != toPh.isPlosive())? 1:0;
                diff += (fromPh.isSonorant() != toPh.isSonorant())? 2:0;
                diff += (fromPh.isSyllabic() != toPh.isSyllabic())? 1:0;
                diff += (fromPh.isVoiced() != toPh.isVoiced())? 1:0;
                diff += (fromPh.isVowel() != toPh.isVowel())? 2:0;
                diff += Math.abs(fromPh.sonority() - toPh.sonority());
                
                String key = fromSym + " " + toSym;
                
                this.aligncost.put(key, diff);
            }
        }
    }
    
    
    /**
     * 
     * This computes the alignment that has the lowest distance between two 
     * Strings.
     * 
     * There are three differences to the normal Levenshtein-distance:
     * 
     * 1. Only insertions and deletions are allowed, no replacements (i.e. no 
     *    "diagonal" transitions)
     * 2. insertion costs are dependent on a particular phone on the input side
     *    (the one they are aligned to)
     * 3. deletion is equivalent to a symbol on the input side that is not 
     *    aligned. There are costs associated with that.
     *    
     * The method returns the output string with alignment boundaries ('#') 
     * inserted.
     * 
     * @param in
     * @param out
     * @return
     */
    private String distanceAlign(String in, String out ) {
        String[] istr = in.split(Pattern.quote(entrySeparator));
        String[] ostr = out.split(Pattern.quote(entrySeparator));
        String delim = "#";
        
        // distances:
        // 1. previous distance (= previous column in matrix)
        int[] p_d = new int[ostr.length+1];
        // 2. current distance
        int[] d = new int[ostr.length+1];
        // 3. dummy array for swapping, when switching to new column
        int[] _d;
        
        // array indicating if a skip was performed (= if current character has not been aligned)
        // same arrays as for distances
        boolean[] p_sk = new boolean[ ostr.length + 1 ];
        boolean[] sk   = new boolean[ ostr.length + 1 ];
        boolean[] _sk;
        
        // arrays storing the alignments corresponding to distances
        String[] p_al = new String[ ostr.length + 1 ];
        String[] al   = new String[ ostr.length + 1 ];
        String[] _al;
        
        // initialize values
        p_d[0]  = 0;
        p_al[0] = "";
        p_sk[0] = true;

        
        // ... still initializing
        for (int j = 1; j < ostr.length + 1; j++){
            // only possibility first is to align the first letter 
            // of the input string to everything
            p_al[j] = p_al[j-1] + " " + ostr[j-1]; 
            p_d[j] = p_d[j-1] + symDist(istr[0],ostr[j-1]);
            p_sk[j] = false;        
        }
        
        // constant penalty for not aligning a character
        int skConst = this.skipcost;
        
        // align
        // can start at 1, since 0 has been treated in initialization
        for (int i=1; i < istr.length; i++) {
            
            // zero'st row stands for skipping from the beginning on
            d[0] = p_d[0] + skConst;
            al[0] = p_al[0] + " " + delim;
            sk[0] = true;
            
            for (int j = 1 ; j < ostr.length + 1; j++ ) {
                
                // translation cost between symbols ( j-1, because 0 row 
                // inserted for not aligning at beginning)
                int tr_cost = symDist(istr[i], ostr[j-1]);
                
                // skipping cost greater zero if not yet aligned 
                int sk_cost = p_sk[j]? skConst : 0;
                
                if ( sk_cost + p_d[j] < tr_cost + d[j-1]) {
                    // skipping cheaper
                    
                    
                    // cost is cost from previous input char + skipping
                    d[j]  = sk_cost + p_d[j];
                    // alignment is from prev. input + delimiter
                    al[j] = p_al[j] + " " + delim;
                    // yes, we skipped
                    sk[j] = true;
                    
                } else {
                    // aligning cheaper
                                
                    // cost is that from previously aligned output + distance
                    d[j]  = tr_cost + d[j-1];
                    // alignment continues from previously aligned
                    al[j] = al[j-1] + " " + ostr[j-1];
                    // nope, didn't skip
                    sk[j] = false;
                    
                }
            }
            
            // swapping
            _d  = p_d;
            p_d = d;
            d   = _d;

            _sk  = p_sk;
            p_sk = sk;
            sk   = _sk;
            
            _al  = p_al;
            p_al = al;
            al   = _al;
        }
        return p_al[ostr.length];
    }
  
    
    /**
     * 
     * This changes the transcription of a MARYXML document in ALLOPHONES format
     * to match the label sequence given as the "labels" parameter. 
     * The symbols of the original transcription are aligned to corrected 
     * ones, with which they are replaced in turn.
     * 
     * @param allophones the MARYXML document, in ALLOPHONES format
     * @param labels the sequence of label symbols to use, separated by the
     * entry separator as provided by getEntrySeparator().
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws SAXException 
     */
    public void alignXmlTranscriptions(Document allophones, String labels)
    throws SAXException, IOException, ParserConfigurationException
    {
        // get all t and boundary elements
        NodeIterator tokenIt = MaryDomUtils.createNodeIterator(allophones, MaryXML.TOKEN, MaryXML.BOUNDARY);
        List<Element> tokens = new ArrayList<Element>();
        Element e;
        while ((e = (Element) tokenIt.nextNode()) != null) {
            tokens.add(e);
        }
        
        String orig = this.collectTranscription(allophones);
        
        System.out.println("Orig   : "+orig);
        System.out.println("Correct: "+labels);
        
        
        // now we align the transcriptions and split it at the delimiters
        String al = this.distanceAlign(orig.trim(),labels.trim()) + " ";

        System.out.println("Alignments: "+al);
        String[] alignments = al.split("#");
        
        // change the transcription in xml according to the aligned one
        this.changeTranscriptions(allophones, alignments);
    }
    
    /**
     * 
     * This computes a string of phonetic symbols out of an allophones xml:
     * - standard phones are taken from "ph" elements in the document
     * - after each token-element (except those followed by a "boundary"-element), 
     *   a "bnd" symbol is inserted (standing for a possible pause).
     * Entries are separated by the entrySeparator character.
     * 
     * @param doc the document to analyse
     * @return
     */
    private String collectTranscription(Document doc)
    {
        // String storing the original transcription begins with a pause
        StringBuilder orig = new StringBuilder();

        NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.PHONE, MaryXML.BOUNDARY);
        Element e;
        Element prevToken = null;
        boolean prevWasBoundary = false;
        while ((e = (Element) ni.nextNode()) != null) {
            if (e.getTagName().equals(MaryXML.PHONE)) {
                Element token = (Element) MaryDomUtils.getAncestor(e, MaryXML.TOKEN);
                if (token != prevToken && !prevWasBoundary) {
                    if (orig.length() > 0) orig.append(entrySeparator);
                    orig.append(possibleBnd);
                }
                if (orig.length() > 0) orig.append(entrySeparator);
                orig.append(e.getAttribute("p"));
                prevToken = token;
                prevWasBoundary = false;
            } else { // boundary
                if (orig.length() > 0) orig.append(entrySeparator);
                orig.append(possibleBnd);
                prevWasBoundary = true;
            }
        }
        
        return orig.toString();
    }
    
    /**
     * 
     * This changes the transcription according to a given sequence of phonetic
     * symbols (including boundaries and pauses). Boundaries in doc are added 
     * or deleted as necessary to match the pause symbols in alignments.
     * 
     * @param doc the document in which to change the transcriptions
     * @param alignments the aligned symbols to use in the update.
     */
    private void changeTranscriptions(Document doc, String[] alignments)
    {
        // Algorithm:
        // * Go through <ph> and <boundary> elements in doc on the one hand,
        // and through alignments on the other hand.
        //   - Special steps for the first <ph> in a token:
        //     -> if the <ph> is the first <ph> in the current token,
        //        and alignment is a pause symbol,
        //        insert a new boundary before the token, and skip the alignment entry;
        //     -> if the <ph> is the first <ph> in the current token,
        //        and the alignment entry is empty, skip the alignment entry.
        //   - for <ph> elements:
        //     -> if the alignment entry is empty, delete the <ph> and,
        //        if it was the only <ph> in the current <syllable>, also
        //        delete the syllable;
        //     -> else, use the current alignment entry, adding any <ph>
        //        elements as necessary.
        //   - for <boundary> elements:
        //     -> if symbol is pause, keep boundary;
        //     -> if symbol is word separator, delete boundary.

        NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.PHONE, MaryXML.BOUNDARY);
        List<Element> origPhonesAndBoundaries = new ArrayList<Element>();
        // We make a copy of the list of original entries, because when
        // we add/remove entries later, that get the node iterator confused.
        Element elt;
        while ((elt = (Element) ni.nextNode()) != null) {
            origPhonesAndBoundaries.add(elt);
        }
        int iAlign = 0;
        Element prevToken = null;
        boolean prevWasBoundary = false;
        for (Element e : origPhonesAndBoundaries) {
            if (e.getTagName().equals(MaryXML.PHONE)) {
                boolean betweenTokens = false;
                Element token = (Element) MaryDomUtils.getAncestor(e, MaryXML.TOKEN);
                if (token != prevToken && !prevWasBoundary) {
                    betweenTokens = true;
                }
                prevToken = token;
                prevWasBoundary = false;
                if (betweenTokens) {
                    if (alignments[iAlign].trim().equals(possibleBnd)) {
                        // Need to insert a boundary before token
                        System.out.println("  inserted boundary in xml");
                        Element b = MaryXML.createElement(doc, MaryXML.BOUNDARY);
                        b.setAttribute("breakindex", "3");
                        token.getParentNode().insertBefore(b, token);
                    }
                    iAlign++; // move beyond the marker between tokens
                }
                System.out.println("Ph = "+e.getAttribute("p")+", align = "+ alignments[iAlign]);
                if (alignments[iAlign].trim().equals("")) {
                    // Need to delete the current <ph> element
                    Element syllable = (Element) e.getParentNode();
                    assert syllable != null;
                    assert syllable.getTagName().equals(MaryXML.SYLLABLE);
                    syllable.removeChild(e);
                    if (MaryDomUtils.getFirstElementByTagName(syllable, MaryXML.PHONE) == null) {
                        // Syllable is now empty, need to delete it as well
                        syllable.getParentNode().removeChild(syllable);
                    }
                } else {
                    // Replace <ph>, add siblings if necessary
                    String[] newPh = alignments[iAlign].trim().split("\\s+");
                    e.setAttribute("p", newPh[0]);
                    if (newPh.length > 1) {
                        // any ph to be added
                        Element syllable = (Element) e.getParentNode();
                        assert syllable != null;
                        assert syllable.getTagName().equals(MaryXML.SYLLABLE);
                        Node rightNeighbor = e.getNextSibling(); // can be null
                        for (int i=1; i<newPh.length; i++) {
                            Element newPhElement = MaryXML.createElement(doc, MaryXML.PHONE);
                            newPhElement.setAttribute("p", newPh[i]);
                            syllable.insertBefore(newPhElement, rightNeighbor);
                        }
                    }
                }
            } else { // boundary
                System.out.println("Boundary, align = "+ alignments[iAlign]);
                if (alignments[iAlign].trim().equals(possibleBnd)) {
                    // keep boundary
                } else {
                    // delete boundary
                    System.out.println("  deleted boundary from xml");
                    e.getParentNode().removeChild(e);
                }
                prevWasBoundary = true;
            }
            iAlign++;
        }
        updatePhAttributesFromPhElements(doc);
    }
    
    
    private void updatePhAttributesFromPhElements(Document doc)
    {
        NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.TOKEN);
        Element t;
        while ((t = (Element) ni.nextNode()) != null) {
            updatePhAttributesFromPhElements(t);
        }
    }

    private void updatePhAttributesFromPhElements(Element token)
    {
        if (token == null) throw new NullPointerException("Got null token");
        if (!token.getTagName().equals(MaryXML.TOKEN)) {
            throw new IllegalArgumentException("Argument should be a <"+MaryXML.TOKEN+">, not a <"+token.getTagName()+">");
        }
        StringBuilder tPh = new StringBuilder();
        TreeWalker sylWalker = MaryDomUtils.createTreeWalker(token, MaryXML.SYLLABLE);
        Element syl;
        while ((syl = (Element) sylWalker.nextNode()) != null) {
            StringBuilder sylPh = new StringBuilder();
            String stress = syl.getAttribute("stress");
            if (stress.equals("1")) sylPh.append("'");
            else if (stress.equals("2")) sylPh.append(",");
            TreeWalker phWalker = MaryDomUtils.createTreeWalker(syl, MaryXML.PHONE);
            Element ph;
            while ((ph = (Element) phWalker.nextNode()) != null) {
                if (sylPh.length() > 0) sylPh.append(" ");
                sylPh.append(ph.getAttribute("p"));
            }
            String sylPhString = sylPh.toString();
            syl.setAttribute("ph", sylPhString);
            if (tPh.length() > 0) tPh.append(" - ");
            tPh.append(sylPhString);
            if (syl.hasAttribute("tone")) {
                tPh.append(" "+syl.getAttribute("tone"));
            }
        }
        token.setAttribute("ph", tPh.toString());
    }


    private int getMaxCost()
    {
        if (aligncost.isEmpty()) return defaultcost;
        int maxMapping = Collections.max(aligncost.values());
        return (maxMapping > defaultcost) ? maxMapping : defaultcost;
    }
    
    
    
    private int symDist(String aString1, String aString2) {
        
        String key = aString1 + " " + aString2;
        
        // if a value is stored, return it
        if (this.aligncost.containsKey(key)) {
            return aligncost.get(key);
        } else if (aString1.equals(aString2)) {
            return 0;
        } else if (aString1.equals(possibleBnd) || aString2.equals(possibleBnd)) {
            // one but not the other is a possible boundary:
            return defaultBoundaryCost;
        }
        return defaultcost;
    }

}
