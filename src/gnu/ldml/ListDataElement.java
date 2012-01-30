/*
 * gnu.ldml.ListDataElement Copyright (C) 2004, 2012 Free Software Foundation,
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ListDataElement extends Element
{
  private Map<String,SortedSet<Leaf>> listData = new HashMap<String,SortedSet<Leaf>>();
  private Map<String,DetailedListElement> listElms = new HashMap<String,DetailedListElement>();

  public ListDataElement(Parser p, Element parent, String name)
  {
    super(p, parent, name);
  }

  public HashMap<String,Element> flattenLeaf(String name)
  {
    Element listObject = listElms.get(name);
    if (listObject == null)
      return null;
    HashMap<String,Element> table = new HashMap<String,Element>();
    ArrayList<Element> stack = new ArrayList<Element>();
    int stackSize;
    stack.add(listObject);
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

  public Iterator<String> leaves()
  {
    return listData.keySet().iterator();
  }

  /**
   * Adds a piece of data with the specified type name.
   *
   * @param typeName the type name, used as the key for data lookup.
   * @param data the data to add.
   * @return the previous data stored under that type name, or
   *         {@code null} if it was unused.
   */
  public SortedSet<Leaf> addData(String typeName, Leaf data)
  {
    SortedSet<Leaf> set = listData.get(typeName);
    if (set == null)
      {
        set = new TreeSet<Leaf>();
        listData.put(typeName, set);
      }
    set.add(data);
    return set;
  }

  /**
   * Adds an element with the specified type name.
   *
   * @param typeName the type name, used as the key for element lookup.
   * @param elm the element to add.
   * @return the previous data stored under that type name, or
   *         {@code null} if it was unused.
   */
  public DetailedListElement addElement(String typeName, DetailedListElement elm)
  {
    return listElms.put(typeName, elm);
  }

  public Element getElement(String typeName)
  {
    return listElms.get(typeName);
  }

  public SortedSet<Leaf> getData(String typeName)
  {
    return listData.get(typeName);
  }

  public Iterator<String> elmKeys()
  {
    return listElms.keySet().iterator();
  }

  public Map<String,SortedSet<Leaf>> getData()
  {
    return listData;
  }

  @Override
  public String toString()
  {
    StringBuilder buffer = new StringBuilder(super.toString());
    int length = buffer.length();
    buffer.replace(length - 1, length, ",listData=");
    buffer.append(listData);
    buffer.append(",listElms=");
    buffer.append(listElms);
    buffer.append("]");
    return buffer.toString();
  }

}
