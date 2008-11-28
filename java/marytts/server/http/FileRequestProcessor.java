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

package marytts.server.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import marytts.util.io.FileUtils;

import org.apache.http.HttpResponse;

/**
 * Processor class for file http requests to Mary server
 * 
 * @author Oytun T&uumlrk
 */
public class FileRequestProcessor extends BaselineRequestProcessor {
    
    public FileRequestProcessor()
    {
        super();
        
        //Add extra initialisations here
    }

    public boolean sendResourceAsStream(String resourceFilename, HttpResponse response)
    {
        boolean ok = false;
        InputStream stream = MaryHttpServer.class.getResourceAsStream(resourceFilename);

        if (stream!=null)
        {
            logger.debug("Resource stream requested by client: " + resourceFilename);

            try {
                MaryHttpServerUtils.toHttpResponse(stream, response, "text/plain");
                ok = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                ok = false;
            }
        }
        
        return ok;
    }
}
