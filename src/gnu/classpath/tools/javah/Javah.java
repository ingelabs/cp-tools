/* gnu.classpath.tools.javah.Javah
 Copyright (C) 2005 Free Software Foundation, Inc.

 This file is part of GNU Classpath.

 GNU Classpath is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2, or (at your option)
 any later version.

 GNU Classpath is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with GNU Classpath; see the file COPYING.  If not, write to the
 Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 02111-1307 USA. */
package gnu.classpath.tools.javah;

import gnu.classpath.tools.Util;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * 
 * @author C. Brian Jones (cbj@gnu.org)
 */
public abstract class Javah
{
  private static final String JAVAH_IMPL = "gnu.classpath.tools.javah";

  public final static Javah getInstance() throws ClassNotFoundException
  {
    String impl = "gnu.classpath.tools.javah.GnuByteCodeJavah";
    String userImpl = System.getProperty(JAVAH_IMPL);
    if (userImpl != null)
      impl = userImpl;
    Class implClass = Class.forName(impl);
    try
      {
        Object obj = implClass.newInstance();
        if (obj instanceof Javah)
          return (Javah) obj;
      }
    catch (Throwable t)
      {
      }
    throw new ClassNotFoundException("Unable to create instance of " + impl);
  }
  private String output_directory = null;
  private String output_file = null;
  private boolean output_jni = false;
  private boolean output_stubs = false;
  private boolean output_verbose = false;
  private Util util = new Util();

  /**
   * Escapes the '[' character with '_3'. Useful only for descriptors, as in
   * method signatures
   */
  public final String escapeArray(String s)
  {
    StringBuffer buf = new StringBuffer(s);
    int start = -1;
    while ((start = s.indexOf('[', start + 1)) != -1)
      {
        buf.replace(start, start + 1, "_3");
        s = buf.toString();
      }
    s = buf.toString();
    return s;
  }

  /**
   * Escapes the ';' character with '_2'. Useful only for descriptors, as in
   * method signatures.
   */
  public final String escapeSemicolon(String s)
  {
    StringBuffer buf = new StringBuffer(s);
    int start = -1;
    while ((start = s.indexOf(';', start + 1)) != -1)
      {
        buf.replace(start, start + 1, "_2");
        s = buf.toString();
      }
    s = buf.toString();
    return s;
  }

  /**
   * Escapes any '_' character with '_1'
   */
  public final String escapeUnderscore(String s)
  {
    StringBuffer buf = new StringBuffer(s);
    int start = -1;
    while ((start = s.indexOf('_', start + 1)) != -1)
      {
        buf.replace(start, start + 1, "_1");
        s = buf.toString();
      }
    s = buf.toString();
    return s;
  }

  /**
   * Escapes any Unicode character XXXX with '_0XXXX'
   */
  public final String escapeUnicode(String s)
  {
    StringBuffer buf = new StringBuffer(s);
    int start = -1;
    while ((start = s.indexOf("\\u", start + 1)) != -1)
      {
        if (s.length() > start + 5)
          {
            buf.replace(start, start + 2, "_0");
            s = buf.toString();
          }
      }
    s = buf.toString();
    return s;
  }

  /**
   * Escapes any '$' with _00024
   */
  public final String escapeUnicodeInner(String s)
  {
    StringBuffer buf = new StringBuffer(s);
    int start = -1;
    while ((start = s.indexOf("$", start + 1)) != -1)
      {
        buf.replace(start, start + 1, "_00024");
        s = buf.toString();
      }
    s = buf.toString();
    return s;
  }

  public InputStream findClass(String className) throws ClassNotFoundException
  {
    return util.findClass(className);
  }

  public final String getJNIType(String jtype)
  {
    String ntype = null;
    boolean isArray = false;
    if (jtype.indexOf("[]") != -1)
      {
        jtype = jtype.substring(0, jtype.indexOf("[]"));
        isArray = true;
      }
    if (jtype.equals("void"))
      ntype = "void";
    else if (jtype.equals("boolean"))
      ntype = "jboolean";
    else if (jtype.equals("byte"))
      ntype = "jbyte";
    else if (jtype.equals("char"))
      ntype = "jchar";
    else if (jtype.equals("short"))
      ntype = "jshort";
    else if (jtype.equals("int"))
      ntype = "jint";
    else if (jtype.equals("long"))
      ntype = "jlong";
    else if (jtype.equals("float"))
      ntype = "jfloat";
    else if (jtype.equals("double"))
      ntype = "jdouble";
    if (isArray)
      {
        if (ntype == null)
          ntype = "jobject";
        ntype = ntype + "Array";
      }
    if (ntype != null)
      return ntype;
    ntype = "jobject";
    if (jtype.equals("java.lang.String"))
      ntype = "jstring";
    else if (jtype.equals("java.lang.Throwable"))
      ntype = "jthrowable";
    else if (jtype.equals("java.lang.Class"))
      ntype = "jclass";
    return ntype;
  }

  public final String getOutputDirectory()
  {
    return output_directory;
  }

  public final String getOutputFile()
  {
    return output_file;
  }

  public final String getSearchPath()
  {
    return util.getSearchPath();
  }

  /**
   * Copied largely from gnu.bytecode.ClassTypeWriter
   */
  public final String getUnicodeName(String s)
  {
    StringBuffer namebuf = new StringBuffer();
    int len = s.length();
    for (int i = 0; i < len; i++)
      {
        char ch = s.charAt(i);
        if (ch >= ' ' && ch < 127)
          namebuf.append(ch);
        else
          {
            namebuf.append("\\u");
            for (int j = 4; --j >= 0;)
              namebuf.append(Character.forDigit((ch >> (j * 4)) & 15, 16));
          }
      }
    return namebuf.toString();
  }

  public final boolean isOutputJNI()
  {
    return output_jni;
  }

  public final boolean isOutputStubs()
  {
    return output_stubs;
  }

  public final boolean isOutputVerbose()
  {
    return output_verbose;
  }

  /**
   * Providers will implement this method to appropriately print the given class
   * to the provided output stream in javah format. This format depends upon the
   * command line options specified.
   * 
   * @param className The name of the class that would be examined
   * @param out The output stream to write to
   */
  public abstract void printClassFile(String className, PrintStream out);

  public final void setClasspath(String path)
  {
    util.setClasspath(path);
  }

  public final void setOutputDirectory(String output_directory)
  {
    this.output_directory = output_directory;
  }

  public final void setOutputFile(String output_file)
  {
    this.output_file = output_file;
  }

  public final void setOutputJNI(boolean output_jni)
  {
    this.output_jni = output_jni;
  }

  public final void setOutputStubs(boolean output_stubs)
  {
    this.output_stubs = output_stubs;
  }

  public final void setOutputVerbose(boolean output_verbose)
  {
    this.output_verbose = output_verbose;
  }
}