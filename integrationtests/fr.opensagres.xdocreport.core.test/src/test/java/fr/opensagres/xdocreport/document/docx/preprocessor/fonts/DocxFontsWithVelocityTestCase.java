/**
 * Copyright (C) 2011-2015 The XDocReport Team <xdocreport@googlegroups.com>
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package fr.opensagres.xdocreport.document.docx.preprocessor.fonts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.docx.DocxConstants;
import fr.opensagres.xdocreport.document.docx.preprocessor.dom.DOMFontsPreprocessor;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;

public class DocxFontsWithVelocityTestCase
{
    @Test
    public void testFonts()
        throws IOException, XDocReportException
    {
        // 1) Load Docx file by filling Velocity template engine and cache
        // it to the registry
        InputStream in = DocxFontsWithVelocityTestCase.class.getResourceAsStream( "DocxFontsWithVelocity.docx" );
        IXDocReport report = XDocReportRegistry.getRegistry().loadReport( in, TemplateEngineKind.Velocity );

        report.addPreprocessor( DocxConstants.WORD_DOCUMENT_XML_ENTRY, DOMFontsPreprocessor.INSTANCE );
        report.addPreprocessor( DocxConstants.WORD_HEADER_XML_ENTRY, DOMFontsPreprocessor.INSTANCE );
        report.addPreprocessor( DocxConstants.WORD_FOOTER_XML_ENTRY, DOMFontsPreprocessor.INSTANCE );

        // 2) Create context Java model
        IContext context = report.createContext();
        context.put( "name", "word" );

        // Change every font name+size with Magneto + 36
        context.put( DOMFontsPreprocessor.FONT_NAME_KEY, "Magneto" );
        context.put( DOMFontsPreprocessor.FONT_SIZE_KEY, 36 );

        // 3) Generate report by merging Java model with the Docx
        File out = new File( "target" );
        out.mkdirs();
        report.process( context, new FileOutputStream( new File( out, "DocxFontsWithVelocity_Out.docx" ) ) );

    }
}
