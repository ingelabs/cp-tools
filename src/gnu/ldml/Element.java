/*
 * gnu.ldml.Element
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

import java.util.ArrayList;

public class Element
{
  public static final Element ROOT = new Element();
  public ArrayList<Element> children = new ArrayList<Element>();
  public String defaultType;
  public String displayName;
  public Parser parentParser;
  public String qualifiedName;
  public Element superElement;
  private Draft draft;
  private String altText;

  private Element()
  {
  }

  /**
   * Constructs a new element from a given parser
   * with the specified parent element, name, approved
   * draft status and no alternative text.
   *
   * @param p the parser which created this.
   * @param parent the parent element.
   * @param name the name of the element.
   */
  public Element(Parser p, Element parent, String name)
  {
    this(p, parent, name, null, null);
  }

  /**
   * Constructs a new element from a given parser
   * with the specified parent element, name, draft
   * status and no alternative text.
   *
   * @param p the parser which created this.
   * @param parent the parent element.
   * @param name the name of the element.
   * @param draft the draft status (optional,
   *        defaults to approved if {@code null}).
   */
  public Element(Parser p, Element parent, String name,
                 Draft draft)
  {
    this(p, parent, name, draft, null);
  }

  /**
   * Constructs a new element from a given parser
   * with the specified parent element, name, draft
   * status and alternative text.
   *
   * @param p the parser which created this.
   * @param parent the parent element.
   * @param name the name of the element.
   * @param draft the draft status (optional,
   *        defaults to approved if {@code null}).
   * @param altText the alternative text (optional,
   *        defaults to the empty string if {@code null}).
   */
  public Element(Parser p, Element parent, String name,
                 Draft draft, String altText)
  {
    parentParser = p;
    superElement = parent;
    qualifiedName = name;
    if (parent != ROOT)
      parent.addChild(this);
    if (draft == null)
      this.draft = Draft.APPROVED;
    else
      this.draft = draft;
    if (altText == null)
      this.altText = "";
    else
      this.altText = altText;
  }

  public void addChild(Element e)
  {
    children.add(e);
  }

  public String getFullName()
  {
    if (superElement == ROOT)
      return qualifiedName;
    return superElement.getFullName() + "." + qualifiedName;
  }

  @Override
  public String toString()
  {
    return getClass().getName() +
      "[defaultType=" + defaultType +
      ",displayName=" + displayName +
      ",parentParser=" + parentParser +
      ",qualifiedName=" + qualifiedName +
      ",superElement=" + superElement.qualifiedName +
      ",draft=" + draft +
      ",altText=" + altText +
      "]";
  }

  /**
   * Returns the alternative text.
   *
   * @return the alternative text.
   */
  public String getAltText()
  {
    return altText;
  }

  /**
   * Returns the draft status.
   *
   * @return the draft status.
   */
  public Draft getDraft()
  {
    return draft;
  }

}
