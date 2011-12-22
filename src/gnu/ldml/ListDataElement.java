/*
 * gnu.ldml.ListDataElement Copyright (C) 2004 Free Software Foundation,
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

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Enumeration;

public class ListDataElement extends Element
{
  public String defaultLeaf;
  public Hashtable<String,Object> listData = new Hashtable<String,Object>();

  public ListDataElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);
  }

  public void addChild(Element e)
  {
    if (e.qualifiedName.equals("default"))
      defaultLeaf = e.defaultType;
  }

  public Hashtable<String,Element> flattenLeaf(String name)
  {
    Object listObject = listData.get(name);
    if (listObject == null)
      return null;
    if (!(listObject instanceof Element))
      throw new Error("Cannot flatten a tree not constitued of Elements");
    Hashtable<String,Element> table = new Hashtable<String,Element>();
    ArrayList<Element> stack = new ArrayList<Element>();
    int stackSize;
    stack.add((Element) listObject);
    while (stack.size() != 0)
      {
        stackSize = stack.size();
        for (int i = 0; i < stackSize; i++)
          {
            Element elt = stack.get(i);
            if (elt.children.size() != 0)
              {
                stack.addAll(elt.children);
              }
            table.put(elt.getFullName(), elt);
          }
        stack.subList(0, stackSize).clear();
      }
    return table;
  }

  public Enumeration<String> leaves()
  {
    return listData.keys();
  }
}
