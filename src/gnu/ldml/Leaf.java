/*
 * gnu.ldml.Leaf
 * Copyright (C) 2012 Free Software Foundation, Inc.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a leaf node containing data in the parsed
 * XML file.
 *
 * @author Andrew John Hughes (gnu_andrew@member.fsf.org)
 */
public class Leaf
  implements Comparable<Leaf>
{

  public enum Draft
  {
    APPROVED("approved"),
    CONTRIBUTED("contributed"),
    PROVISIONAL("provisional"),
    UNCONFIRMED("unconfirmed");

    private static final Map<String, Draft> stringToEnum =
      new HashMap<String, Draft>();

    static
    {
      for (Draft draft : values())
        stringToEnum.put(draft.toString(), draft);
    }

    private final String draft;

    Draft(final String draft) { this.draft = draft; }
    @Override public String toString() { return draft; }
    public static Draft fromString(String draft)
    {
      return stringToEnum.get(draft);
    }
  }

  /**
   * The data.
   */
  private final String data;

  /**
   * The type of the data.
   */
  private final String type;

  /**
   * The draft status of the data.
   */
  private final Draft draft;

  /**
   * The label for this data, if it is an alternative value.
   */
  private final String alternative;

  /**
   * Constructs a new leaf node with the given data and type,
   * approved draft status and no alternative text.
   *
   * @param data the data.
   * @param type the type of the data.
   */
  public Leaf(String data, String type)
  {
    this(data, type, null, null);
  }

  /**
   * Constructs a new leaf node, with the
   * given data, type, draft status and no
   * alternative value text.
   *
   * @param data the data.
   * @param type the type of the data.
   * @param draft the draft status of the data.
   */
  public Leaf(String data, String type, Draft draft)
  {
    this(data, type, draft, null);
  }

  /**
   * Constructs a new leaf node, with the
   * given data, type, draft status and optional
   * alternative value text.
   *
   * @param data the data.
   * @param type the type of the data.
   * @param draft the draft status of the data.
   * @param alternative the alternative value text,
   *        or {@code null} if there is none.
   */
  public Leaf(String data, String type, Draft draft, String alternative)
  {
    this.data = data;
    this.type = type;
    if (draft == null)
      this.draft = Draft.APPROVED;
    else
      this.draft = draft;
    if (alternative == null)
      this.alternative = "";
    else
      this.alternative = alternative;
  }

  /**
   * Returns a textual representation of this entity.
   *
   * @return a textual representation.
   */
  @Override
  public String toString()
  {
    return getClass().getName() +
      "[data=" + data + ",type=" + type +
      ",draft=" + draft + ",alternative=" + alternative + "]";
  }

  /**
   * Returns a hashcode for this leaf.
   *
   * @return a hashcode.
   */
  @Override
  public int hashCode()
  {
    return (1 * data.hashCode()) +
      (3 * type.hashCode()) +
      (5 * draft.hashCode()) +
      (7 * alternative.hashCode());
  }

  /**
   * Returns true if the specified object is
   * a {@code Leaf} with the same data, draft
   * and alternative text.
   *
   * @param other the other object to compare.
   * @return true if the objects are equal.
   */
  @Override
  public boolean equals(Object other)
  {
    if (other == this)
      return true;
    if (other == null)
      return false;
    if (other instanceof Leaf)
      {
        Leaf oLeaf = (Leaf) other;
        return oLeaf.data.equals(data) &&
          oLeaf.type.equals(type) &&
          oLeaf.draft.equals(draft) &&
          oLeaf.alternative.equals(alternative);
      }
    return false;
  }

  /**
   * Compares this leaf with the given object for
   * order.  Leafs are sorted first by type, then
   * by the presence of alternative text, then by
   * draft status and finally by data.
   *
   * @param other the object to compare.
   * @return a negative integer, zero or a positive
   *         integer depending on if this object is
   *         less than, equal to or greater than the
   *         object respectively.
   * @throws NullPointerException if {@code other} is {@code null}.
   * @throws ClassCastException if {@code other} is not a {@code Leaf}.
   */
  public int compareTo(Leaf other)
  {
    int compare = type.compareTo(other.type);
    if (compare != 0)
      return compare;
    compare = alternative.compareTo(other.alternative);
    if (compare != 0)
      return compare;
    compare = draft.compareTo(other.draft);
    if (compare != 0)
      return compare;
    return data.compareTo(other.data);
  }

  /**
   * Returns the data for this leaf.
   *
   * @return the data.
   */
  public String getData()
  {
    return data;
  }

  /**
   * Returns the type of this leaf.
   *
   * @return the type.
   */
  public String getType()
  {
    return type;
  }

  /**
   * Returns the draft status of this leaf.
   *
   * @return the draft status.
   */
  public Draft getDraftStatus()
  {
    return draft;
  }

  /**
   * Returns the alternative text for this leaf.
   *
   * @return the alternative text.
   */
  public String getAlternative()
  {
    return alternative;
  }

}