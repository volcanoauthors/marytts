/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.adaptation.prosody;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 *  
 *  Pitch statistics (could be for all recordings, for a group of recordings or even for a single utterance):
 *  - Mean of (voiced) f0s
 *  - Standard deviation of (voiced) f0s
 *  - Global minimum of f0s
 *  - Global maximum of f0s
 *  - Average tilt of f0 contours, or tilt of single contour
 *  
 * @author Oytun T&uumlrk
 */
public class PitchStatistics {
    public static int STATISTICS_IN_HERTZ = 1;
    public static int STATISTICS_IN_LOGHERTZ = 2;
    public static int DEFAULT_STATISTICS = STATISTICS_IN_HERTZ;
    public int type;
    
    public boolean isSource;
    public boolean isGlobal;
    
    public double mean;
    public double standardDeviation;
    public double range;
    public double intercept;
    public double slope;
    
    public PitchStatistics(PitchStatistics existing)
    {
        type = existing.type;
        isSource = existing.isSource;
        isGlobal = existing.isGlobal;
        mean = existing.mean;
        standardDeviation = existing.standardDeviation;
        range = existing.range;
        intercept = existing.intercept;
        slope = existing.slope;
    }
    
    public PitchStatistics()
    {
        this(DEFAULT_STATISTICS, true, true);
    }
    
    public PitchStatistics(int typeIn, boolean isSourceIn, boolean isGlobalIn)
    {
        type = typeIn;
        
        init();
        
        isSource = isSourceIn;
        isGlobal = isGlobalIn;
    }
    
    public PitchStatistics(int typeIn, double[] f0s)
    {
        this(typeIn, true, true, f0s);
    }
    
    public PitchStatistics(int typeIn, boolean isSourceIn, boolean isGlobalIn, double[] f0s)
    {
        type = typeIn;
        
        init();
        
        isSource = isSourceIn;
        isGlobal = isGlobalIn;
        
        double[] voiceds = SignalProcUtils.getVoiceds(f0s);
        
        if (type==PitchStatistics.STATISTICS_IN_LOGHERTZ)
            voiceds = SignalProcUtils.getLogF0s(voiceds);
        
        if (voiceds!=null)
        {
            mean = MathUtils.mean(voiceds);
            standardDeviation = MathUtils.standardDeviation(voiceds, mean);
            
            range = SignalProcUtils.getF0Range(voiceds);
            
            double[] contourInt = SignalProcUtils.interpolate_pitch_uv(f0s);
            double[] line = SignalProcUtils.getContourLSFit(contourInt, false);
            intercept = line[0];
            slope = line[1];
        }
        else
        {
            mean = 0.0;
            standardDeviation = 1.0;
        }
    }
    
    public void init()
    {
        mean = 0.0;
        standardDeviation = 0.0;
        range = 0.0;
        intercept = 0.0;
        slope = 0.0;
    }
    
    public void read(MaryRandomAccessFile ler)
    {
        try {
            type = ler.readInt();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            isSource = ler.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            isGlobal = ler.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            mean = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            standardDeviation = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            range = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            intercept = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            slope = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void write(MaryRandomAccessFile ler)
    {
        try {
             ler.writeInt(type);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeBoolean(isSource);
       } catch (IOException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }
       
       try {
           ler.writeBoolean(isGlobal);
      } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
        
        try {
             ler.writeDouble(mean);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeDouble(standardDeviation);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeDouble(range);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeDouble(intercept);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeDouble(slope);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
