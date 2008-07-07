/*
 * gnu.currencygen.Main Copyright (C) 2004 Free Software Foundation,
 * Inc.
 *
 * This file is part of GNU Classpath.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * GNU Classpath is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Classpath; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package gnu.currencygen;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class Main
{

  static public void main(String args[]) throws Exception
  {
    XMLReader reader;
    SupplementalHandler handler;
    InputSource source;
    printVersion();
    if (args.length != 1)
      {
        printUsage();
        return;
      }
    reader = XMLReaderFactory.createXMLReader();
    source = new InputSource(new FileInputStream(args[0]));
    FileWriter currencyFile = new FileWriter("iso4217.properties");
    BufferedWriter bWriter = new BufferedWriter(currencyFile);
    PrintWriter output = new PrintWriter(bWriter, true);
    BufferedWriter weekFile = new BufferedWriter(new FileWriter("weeks.properties"));
    PrintWriter wOutput = new PrintWriter(weekFile, true);
    handler = new SupplementalHandler(output, wOutput);
    reader.setContentHandler(handler);
    reader.parse(source);
    bWriter.flush();
    weekFile.flush();
    bWriter.close();
    weekFile.close();
  }

  static void printUsage()
  {
    System.out.println(" Usage: [filename]");
    System.out.println();
  }
  static void printVersion()
  {
    System.out
      .println(" This is the LDML to GNU Classpath converter (currency part)");
    System.out.println("   Copyright (C) 2004 The Free Software Foundation.");
    System.out.println();
  }
}
class SupplementalHandler extends DefaultHandler
{

  static class CurrencyInfo
  {
    int digits;
    int rounding;

    CurrencyInfo(int digits, int rounding)
    {
      this.digits = digits;
      this.rounding = rounding;
    }
  }
  static final int STATE_ALTERNATE = 6;
  static final int STATE_CURRENCY = 4;
  static final int STATE_CURRENCYDATA = 5;
  static final int STATE_FRACTIONS = 8;
  static final int STATE_IGNORING = 2;
  static final int STATE_INFO = 9;
  static final int STATE_REGION = 3;
  static final int STATE_SEENCURRENCY = 7;
  static final int STATE_SUPPLEMENTAL = 1;
  static final int STATE_WEEKDATA = 10;
  static final int STATE_ZERO = 0;
  Map currencyInfo = new HashMap();
  String currentCurrency;
  String currentRegion;
  int ignoreLevel;
  int oldState;
  PrintWriter output;
  PrintWriter wOutput;
  Map weekInfo = new HashMap();

  int state;

  /**
   *
   * Constructs a new handler for supplemental data.
   *
   * @param output the output file for the currency data.
   * @param wOutput the output file for the week data.
   */
  public SupplementalHandler(PrintWriter output, PrintWriter wOutput)
  {
    this.output = output;
    this.wOutput = wOutput;
  }

  void checkMultiState(int[] currentStates, int newState) throws SAXException
  {
    int i;
    for (i = 0; i < currentStates.length; i++)
      {
        if (currentStates[i] == state)
          break;
      }
    if (i == currentStates.length)
      throw new SAXException("Invalid current state " + state);
    oldState = state;
    state = newState;
  }

  void checkState(int currentState, int newState) throws SAXException
  {
    if (currentState != state)
      throw new SAXException("Invalid current state " + currentState
                             + " (was expecting " + state + ")");
    oldState = state;
    state = newState;
  }

  public void endElement(String uri, String localName, String qName)
    throws SAXException
  {
    if (ignoreLevel > 0)
      {
        ignoreLevel--;
        return;
      }
    if (state == STATE_SEENCURRENCY || state == STATE_REGION)
      {
        output.println();
        CurrencyInfo info = (CurrencyInfo) currencyInfo.get(currentCurrency);
        if (info == null)
          info = (CurrencyInfo) currencyInfo.get("DEFAULT");
        if (info != null)
          {
            output.println(currentRegion + ".fractionDigits=" + info.digits);
          }
      }
    if (localName.equals("supplementalData"))
      checkState(STATE_SUPPLEMENTAL, STATE_ZERO);
    else if (localName.equals("currencyData"))
      checkState(STATE_CURRENCYDATA, STATE_SUPPLEMENTAL);
    else if (localName.equals("region"))
      checkMultiState(new int[] { STATE_SEENCURRENCY, STATE_REGION },
                      STATE_CURRENCYDATA);
    else if (localName.equals("currency"))
      checkState(STATE_CURRENCY, STATE_SEENCURRENCY);
    else if (localName.equals("alternate"))
      checkState(STATE_ALTERNATE, STATE_CURRENCY);
    else if (localName.equals("fractions"))
      checkState(STATE_FRACTIONS, STATE_CURRENCYDATA);
    else if (localName.equals("info"))
      checkState(STATE_INFO, STATE_FRACTIONS);
    else if (localName.equals("weekData"))
      {
	checkState(STATE_WEEKDATA, STATE_SUPPLEMENTAL);
	Iterator iter = weekInfo.entrySet().iterator();
	while (iter.hasNext())
	  {
	    Map.Entry entry = (Map.Entry) iter.next();
	    wOutput.println(entry.getKey() + "=" + entry.getValue());
	  }
      }
  }

  public void startDocument()
  {
    output
      .println("# This document is automatically generated by gnu.currencygen");
    output.println();
    wOutput
      .println("# This document is automatically generated by gnu.currencygen");
    wOutput.println();
    state = STATE_ZERO;
    ignoreLevel = 0;
  }

  public void startElement(String uri, String localName, String qName,
                           Attributes atts) throws SAXException
  {
    if (ignoreLevel > 0)
      {
        ignoreLevel++;
        return;
      }
    if (localName.equals("supplementalData"))
      checkState(STATE_ZERO, STATE_SUPPLEMENTAL);
    else if (localName.equals("currencyData"))
      checkState(STATE_SUPPLEMENTAL, STATE_CURRENCYDATA);
    else if (localName.equals("region"))
      checkState(STATE_CURRENCYDATA, STATE_REGION);
    else if (localName.equals("currency"))
      checkMultiState(new int[] { STATE_SEENCURRENCY, STATE_REGION },
                      STATE_CURRENCY);
    else if (localName.equals("alternate"))
      checkState(STATE_CURRENCY, STATE_ALTERNATE);
    else if (localName.equals("fractions"))
      checkState(STATE_CURRENCYDATA, STATE_FRACTIONS);
    else if (localName.equals("info"))
      checkState(STATE_FRACTIONS, STATE_INFO);
    else if (localName.equals("weekData"))
      checkState(STATE_SUPPLEMENTAL, STATE_WEEKDATA);
    else if (localName.equals("minDays") && state == STATE_WEEKDATA)
      {
	String status = atts.getValue("draft");
	if (status == null || status.equals("false") || status.equals("approved"))
	  {
	    int minDays = Integer.parseInt(atts.getValue("count"));
	    String[] territories = atts.getValue("territories").split(" ");
	    for (int a = 0; a < territories.length; ++a)
	      {
		if (territories[a].equals("001"))
		  wOutput.println("DEFAULT=" + minDays);
		else
		  weekInfo.put(territories[a], minDays);
	      }
	  }
      }
    else
      {
        ignoreLevel++;
        return;
      }
    if (state == STATE_REGION)
      {
        String tRegion = (String) atts.getValue("iso3166");
        if (tRegion == null)
          throw new SAXException("region must have a iso3166 attribute");
        currentRegion = tRegion;
        output.print(tRegion + ".currency=");
      }
    if (state == STATE_INFO)
      {
        String currencyCode = (String) atts.getValue("iso4217");
        String digits = (String) atts.getValue("digits");
        String rounding = (String) atts.getValue("rounding");
        if (currencyCode == null || digits == null || rounding == null)
          throw new SAXException(
                                 "currency info must have an iso4217, a digits and a rounding attribute (here we get "
                                                                  + currencyCode
                                                                  + ","
                                                                  + digits
                                                                  + ","
                                                                  + rounding
                                                                  + ")");
        currencyInfo.put(currencyCode, new CurrencyInfo(Integer
          .parseInt(digits), Integer.parseInt(rounding)));
      }
    if (state == STATE_CURRENCY || state == STATE_ALTERNATE)
      {
        String tName = (String) atts.getValue("iso4217");
        if (tName == null)
          throw new SAXException("currency must have a iso 4217 attribute");
        if (state == STATE_CURRENCY)
          currentCurrency = tName;
        // We only treat current currencies.
        if (atts.getValue("before") == null)
          {
            if (oldState == STATE_SEENCURRENCY || state == STATE_ALTERNATE)
              output.print(',');
            output.print(tName);
          }
        else
          {
            System.err.println("WARNING: before not supported (value="
                               + atts.getValue("before") + ")");
          }
      }
  }
}
