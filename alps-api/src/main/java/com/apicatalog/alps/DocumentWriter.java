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
package com.apicatalog.alps;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.apicatalog.alps.dom.Document;
import com.apicatalog.alps.error.DocumentWriterException;

public interface DocumentWriter {

    default void write(Document document, OutputStream stream) throws IOException, DocumentWriterException {
        write(document, new OutputStreamWriter(stream));
    }
    
    void write(Document document, Writer writer) throws IOException, DocumentWriterException;    
}