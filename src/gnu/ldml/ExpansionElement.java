/*
 * gnu.ldml.ExpansionElement Copyright (C) 2004 Free Software Foundation,
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
package gnu.ldml;

import java.util.Iterator;
import org.xml.sax.SAXException;

public class ExpansionElement extends OrderedListBaseElement
{
  public String extendData;
  public String extendOperator;
  public String extendOperatorData;

  public ExpansionElement(Parser p, Element parent, String qName)
  {
    super(p, parent, qName);
  }

  void fixExpansionData() throws SAXException
  {
    /*
     * Here we look for <extend> and assumes that any other operator is a
     * collation rule
     */
    Iterator iter = children.iterator();
    while (iter.hasNext())
      {
        Element elt = (Element) iter.next();
        if (!(elt instanceof DataElement))
          throw new SAXException("All children of " + qualifiedName
                                 + " should be data element");
        DataElement data_elt = (DataElement) elt;
        if (elt.qualifiedName.equals("extend"))
          {
            extendData = data_elt.data;
          }
        else
          {
            if (extendOperator != null)
              throw new SAXException(
                                     "Only one collation rule by expansion element is authorized");
            extendOperator = data_elt.qualifiedName;
            extendOperatorData = data_elt.data;
          }
      }
  }
}
