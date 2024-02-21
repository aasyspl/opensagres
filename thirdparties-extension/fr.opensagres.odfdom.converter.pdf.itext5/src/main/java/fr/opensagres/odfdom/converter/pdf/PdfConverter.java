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
package fr.opensagres.odfdom.converter.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import fr.opensagres.xdocreport.utils.StringUtils;
import org.apache.xerces.dom.TextImpl;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.odftoolkit.odfdom.dom.OdfStylesDom;
import org.odftoolkit.odfdom.dom.element.text.TextHiddenParagraphElement;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeMasterStyles;
import org.odftoolkit.odfdom.pkg.OdfElement;

import fr.opensagres.odfdom.converter.core.AbstractODFConverter;
import fr.opensagres.odfdom.converter.core.IODFConverter;
import fr.opensagres.odfdom.converter.core.ODFConverterException;
import fr.opensagres.odfdom.converter.pdf.internal.ElementVisitorForIText;
import fr.opensagres.odfdom.converter.pdf.internal.StyleEngineForIText;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PdfConverter
    extends AbstractODFConverter<PdfOptions>
{

    private static final IODFConverter<PdfOptions> INSTANCE = new PdfConverter();

    public static IODFConverter<PdfOptions> getInstance()
    {
        return INSTANCE;
    }

    @Override
    protected void doConvert( OdfDocument odfDocument, OutputStream out, Writer writer, PdfOptions options )
        throws ODFConverterException, IOException
    {
        try
        {
            // process styles
            StyleEngineForIText styleEngine = processStyles( odfDocument, options );

            // process content
            ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
            ElementVisitorForIText visitorForIText = processBody( odfDocument, tempOut, styleEngine, options, null );
            Integer expectedPageCount = visitorForIText.getExpectedPageCount();
            int actualPageCount = visitorForIText.getActualPageCount();
            if ( expectedPageCount == null || expectedPageCount == actualPageCount )
            {
                // page count not required or correct, copy temp stream to output stream
                out.write( tempOut.toByteArray() );
                out.close();
            }
            else
            {
                // page count inconsistent, do second visit with forced page count
                processBody( odfDocument, out, styleEngine, options, actualPageCount );
            }
        }
        catch ( Exception e )
        {
               e.printStackTrace();
            throw new ODFConverterException( e );
        }
    }

    private StyleEngineForIText processStyles( OdfDocument odfDocument, PdfOptions options )
        throws Exception
    {
        StyleEngineForIText styleEngine = new StyleEngineForIText( odfDocument, options );

        OdfStylesDom stylesDom = odfDocument.getStylesDom();
        OdfContentDom contentDom = odfDocument.getContentDom();

        // 1.1) Parse
        // styles.xml//office:document-styles/office:styles
        stylesDom.getOfficeStyles().accept( styleEngine );

        // 1.2) Parse
        // styles.xml//office:document-styles/office:automatic-styles
        stylesDom.getAutomaticStyles().accept( styleEngine );

        // 1.3) Parse
        // content.xml//office:document-content/office:automatic-styles
        contentDom.getAutomaticStyles().accept( styleEngine );

        return styleEngine;
    }

    private ElementVisitorForIText processBody( OdfDocument odfDocument, OutputStream out,
                                                StyleEngineForIText styleEngine, PdfOptions options,
                                                Integer forcedPageCount )
        throws Exception
    {
        ElementVisitorForIText visitorForIText;

        if ( options != null && options instanceof PdfAOptions )
        {
            visitorForIText =
                new ElementVisitorForIText( odfDocument, out, styleEngine, ( PdfAOptions ) options, forcedPageCount );
        }
        else
        {
            visitorForIText =
                new ElementVisitorForIText( odfDocument, out, styleEngine, options, forcedPageCount );
        }

        OdfOfficeMasterStyles masterStyles = odfDocument.getOfficeMasterStyles();
        OdfElement contentRoot = odfDocument.getContentRoot();

        processHiddenElements(contentRoot);

        // 2.1) Parse
        // styles.xml//office:document-styles/office:master-styles
        masterStyles.accept( visitorForIText );

        // 2.2) Parse
        // content.xml//office:body
        contentRoot.accept( visitorForIText );

        visitorForIText.save();

        return visitorForIText;
    }

    private void processHiddenElements(OdfElement contentRoot) {
        processHiddenParagraph(contentRoot);
    }

    private void processHiddenParagraph(OdfElement contentRoot){
        NodeList hiddenParagraphElementList;
        do {
            hiddenParagraphElementList = contentRoot.getElementsByTagName("text:hidden-paragraph");
            if(hiddenParagraphElementList.getLength() > 0){
                TextHiddenParagraphElement item = (TextHiddenParagraphElement)hiddenParagraphElementList.item(0);
                String conditionString = item.getTextConditionAttribute();
                if(StringUtils.isNotEmpty(conditionString) && conditionString.contains("ooow:")) {
                    conditionString = conditionString.replace("ooow:", "");
                    try {
                        Boolean result = new SpelExpressionParser().parseExpression(conditionString).getValue(Boolean.class);
                        if (result.booleanValue()) {
                            //find the tag and remove all because tag should be hidden
                            Node parentNode = findParentByTagName(item, "text:p");
                            if(parentNode != null){
                                parentNode.setTextContent("");
                                parentNode.getParentNode().removeChild(parentNode);
                            }
                        } else {
                            //remove only hidden tag
                            item.getParentNode().removeChild(item);
                        }
                    }catch(Exception e)
                    {
                        item.setNodeValue(conditionString + e.getMessage() + e.getStackTrace());
                    }
                }

            }
        }while(hiddenParagraphElementList != null && hiddenParagraphElementList.getLength() > 0);

    }

    private Node findParentByTagName(Node item, String tagName){
        if(item == null){
            return item;
        }
        Node parentNode = item.getParentNode();
        while(parentNode != null && !parentNode.getNodeName().equalsIgnoreCase(tagName))  {
            parentNode = parentNode.getParentNode();
        }
        return parentNode;
    }
}
