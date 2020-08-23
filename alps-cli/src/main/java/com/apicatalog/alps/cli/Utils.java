/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apicatalog.alps.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.apicatalog.alps.DocumentParser;
import com.apicatalog.alps.DocumentWriter;
import com.apicatalog.alps.jsonp.JsonDocumentParser;
import com.apicatalog.alps.jsonp.JsonDocumentWriter;
import com.apicatalog.alps.xml.XmlDocumentParser;
import com.apicatalog.alps.xml.XmlDocumentWriter;

final class Utils {

    private Utils() {
    }
    
    static final String getMediaType(final String type, final String path, final boolean input) {
        
        if (Constants.ARG_PARAM_XML.equalsIgnoreCase(type)) {
            return Constants.MEDIA_TYPE_ALPS_XML;
        }

        if (Constants.ARG_PARAM_JSON.equalsIgnoreCase(type)) {
            return Constants.MEDIA_TYPE_ALPS_JSON;
        }

        if (type != null) {
            throw new IllegalArgumentException("Unknown type [" + type + "], expected [xml] or [json].");
        }
        
        if (path != null && (path.toLowerCase().endsWith(".xml") || path.toLowerCase().endsWith("+xml"))) {
            return Constants.MEDIA_TYPE_ALPS_XML;
        }

        if (path != null && (path.toLowerCase().endsWith(".json") || path.toLowerCase().endsWith("+json"))) {
            return Constants.MEDIA_TYPE_ALPS_JSON;
        }
        
        if (path != null) {
            throw new IllegalArgumentException("Can not determine " + (input ? "input" : "output") + " file type [" + path + "], please add --" + (input ? "source" : "target")  + "=(json|xml) argument.");
        }

        throw new IllegalArgumentException("Can not determine " + (input ? "input" : "output") + " type, please add --" + (input ? "source" : "target")  + "=(json|xml) argument.");
    }

    static final InputStream fileToInputStream(final String path) {
        
        final File file = new File(path);
        
        if (!file.exists()) {
            System.err.println("Input file '" + path + "' does not exist.");            
            return null;
        }

        if (!file.canRead()) {
            System.err.println("Input file '" + path + "' is not readable.");
            return null;
        }
        
        try {
            return new FileInputStream(file);
            
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        
        return null;
    }

    static final DocumentParser getParser(final String mediaType) {
        
        if ("application/alps+json".equals(mediaType)) {
            return new JsonDocumentParser();
        }

        if ("application/alps+xml".equals(mediaType)) {
            return new XmlDocumentParser();
        }

        throw new IllegalArgumentException("Unsupported source media type [" + mediaType + "].");
    }
    
    static final DocumentWriter getWriter(final String mediaType, final boolean prettyPrint, final boolean verbose) {
        
        if ("application/alps+json".equals(mediaType)) {
            return JsonDocumentWriter.create(prettyPrint, verbose);
        }

        if ("application/alps+xml".equals(mediaType)) {
            return XmlDocumentWriter.create(prettyPrint, verbose);
        }

        throw new IllegalArgumentException("Unsupported target media type [" + mediaType + "].");
    }    

}
