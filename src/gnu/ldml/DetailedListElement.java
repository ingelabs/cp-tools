/*
 * gnu.ldml.DetailedListElement
 * Copyright (C) 2004, 2012 Free Software Foundation,
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

public class DetailedListElement extends Element
{

  public DetailedListElement(Parser p, ListDataElement parent, String name,
                             String typeName)
  {
    super(p, parent, name);
    DetailedListElement elm = parent.addElement(typeName, this);
    if (elm != null)
      throw new IllegalArgumentException("typeName " + typeName + " already in use.");
  }

  public String getFullName()
  {
    return qualifiedName;
  }
}
