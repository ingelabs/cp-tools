/* gnu.classpath.tools.javah.GnuByteCodeJavah
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

import gnu.bytecode.Access;
import gnu.bytecode.Attribute;
import gnu.bytecode.ClassFileInput;
import gnu.bytecode.ClassType;
import gnu.bytecode.ConstantValueAttr;
import gnu.bytecode.Field;
import gnu.bytecode.Method;
import gnu.bytecode.Type;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;

/**
 * 
 * @author C. Brian Jones (cbj@gnu.org)
 */
public class GnuByteCodeJavah extends Javah
{

  private String getJNIMethodName(ClassType classType, Method m)
  {
    StringBuffer result = new StringBuffer();
    String classname = classType.getName();
    classname = classname.replace('.', '_');
    result.append("Java_");
    result.append(classname);
    result.append("_");
    Method method = classType.getMethods();
    int overload = 0;
    while (method != null)
      {
        int flags = method.getModifiers();
        if ((flags & Access.NATIVE) != 0)
          {
            if (method.getName().equals(m.getName()))
              overload++;
          }
        method = method.getNext();
      }
    String methodname = getUnicodeName(m.getName());
    methodname = escapeUnderscore(methodname);
    methodname = escapeUnicode(methodname);
    result.append(methodname);
    if (overload > 1)
      {
        result.append("__");
        StringBuffer sig = new StringBuffer(m.getSignature());
        String signature = sig.toString();
        int idx = signature.indexOf('(');
        if (idx != -1)
          sig.deleteCharAt(idx);
        signature = sig.toString();
        idx = signature.indexOf(')');
        if (idx != -1)
          sig.delete(idx, sig.length());
        signature = sig.toString();
        signature = escapeUnderscore(signature);
        signature = escapeSemicolon(signature);
        signature = escapeArray(signature);
        signature = signature.replace('/', '_');
        result.append(signature);
      }
    return result.toString();
  }
  /**
   * @see gnu.classpath.tools.javah.Javah#printClassFile(java.lang.String,
   *      java.io.PrintStream)
   */
  public void printClassFile(String className, PrintStream out)
  {
    StringBuffer buf;
    ClassType classType = null;
    if (isOutputVerbose())
      {
        buf = new StringBuffer();
        buf.append("[Search path = ");
        String path = getSearchPath();
        buf.append(path);
        buf.append("]");
        out.println(buf.toString());
      }
    try
      {
        InputStream is = findClass(className);
        classType = ClassFileInput.readClassType(is);
      }
    catch (Throwable t)
      {
        out.println("Error: Class " + className + " could not be found.");
        return;
      }
    if (getOutputDirectory() != null)
      {
        String filename = className.replace('.', '_');
        filename = filename.replace('$', '_');
        filename = filename + ".h";
        File dir = new File(getOutputDirectory());
        // fatal error
        if (!dir.exists())
          return;
        File f = new File(dir, className);
        if (f.exists())
          if (!f.delete())
            {
              System.err.println("Error: The file " + f.getPath()
                                 + " could not be deleted.");
              return;
            }
        if (isOutputVerbose())
          {
            buf = new StringBuffer();
            buf.append("[Creating ");
            buf.append(f.getPath());
            buf.append("]");
            out.println(buf.toString());
          }
        writeHeader(classType, f);
      }
    else if (getOutputFile() != null)
      {
        File f = new File(getOutputFile());
        if (isOutputVerbose())
          {
            buf = new StringBuffer();
            buf.append("[Creating ");
            buf.append(f.getPath());
            buf.append("]");
            out.println(buf.toString());
          }
        writeHeader(classType, f);
      }
  }

  private void writeHeader(ClassType classType, File f)
  {
    StringBuffer buf;
    String linesep = System.getProperty("line.separator");
    boolean newfile = f.exists();
    try
      {
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.seek(raf.length());
        FileWriter writer = new FileWriter(raf.getFD());
        if (!newfile)
          {
            buf = new StringBuffer();
            buf.append("/* DO NOT EDIT THIS FILE - it is machine generated */");
            buf.append(linesep);
            buf.append("#include <jni.h>");
            buf.append(linesep);
            writer.write(buf.toString());
          }
        buf = new StringBuffer();
        buf.append("/* Header for class ");
        String className = classType.getName();
        className = getUnicodeName(className);
        className = escapeUnicode(className);
        className = className.replace('$', '_');
        buf.append(className.replace('.', '_'));
        buf.append(" */");
        buf.append(linesep);
        buf.append(linesep);
        writer.write(buf.toString());
        String includedef = "_Included_" + className.replace('.', '_');
        buf = new StringBuffer();
        buf.append("#ifndef ");
        buf.append(includedef);
        buf.append(linesep);
        buf.append("#define ");
        buf.append(includedef);
        buf.append(linesep);
        buf.append("#ifdef __cplusplus");
        buf.append(linesep);
        buf.append("extern \"C\" {");
        buf.append(linesep);
        buf.append("#endif");
        buf.append(linesep);
        writer.write(buf.toString());
        // write any static fields
        Field field = classType.getFields();
        while (field != null)
          {
            int flags = field.getModifiers();
            if ((flags & Access.STATIC) != 0)
              {
                buf = new StringBuffer();
                String fieldname = field.getSourceName();
                fieldname = getUnicodeName(fieldname);
                fieldname = escapeUnicode(fieldname);
                if ((flags & Access.FINAL) != 0)
                  {
                    ConstantValueAttr constval = (ConstantValueAttr) Attribute
                      .get(field, "ConstantValue");
                    if (constval == null)
                      {
                        //        System.out.println ("DBG1: " + fieldname);
                        buf.append("/* Inaccessible static: ");
                        buf.append(escapeUnicodeInner(fieldname));
                        buf.append(" */");
                        buf.append(linesep);
                        writer.write(buf.toString());
                        field = field.getNext();
                        continue;
                      }
                    Object val = constval.getValue(classType.getConstants());
                    if (val instanceof String)
                      {
                        System.out.println("DBG2: " + fieldname);
                        field = field.getNext();
                        continue;
                      }
                    fieldname = className.replace('.', '_') + "_" + fieldname;
                    buf.append("#undef ");
                    buf.append(escapeUnicodeInner(fieldname));
                    buf.append(linesep);
                    buf.append("#define ");
                    buf.append(fieldname);
                    buf.append(" ");
                    if (val instanceof Integer)
                      {
                        Integer valint = (Integer) val;
                        buf.append(valint.toString());
                        buf.append("L");
                      }
                    else if (val instanceof Long)
                      {
                        Long vallong = (Long) val;
                        buf.append(vallong.toString());
                        buf.append("LL");
                      }
                    else if (val instanceof Float)
                      {
                        Float valfloat = (Float) val;
                        buf.append(valfloat.toString());
                        buf.append("f");
                      }
                    else if (val instanceof Double)
                      {
                        Double valdouble = (Double) val;
                        buf.append(valdouble.toString());
                        buf.append("D");
                      }
                    else
                      System.err.println("Unknown constant value " + val);
                    buf.append(linesep);
                  }
                else
                  {
                    buf.append("/* Inaccessible static: ");
                    buf.append(escapeUnicodeInner(fieldname));
                    buf.append(" */");
                    buf.append(linesep);
                  }
                writer.write(buf.toString());
              }
            field = field.getNext();
          }
        // write any native methods out
        Method method = classType.getMethods();
        while (method != null)
          {
            int flags = method.getModifiers();
            if ((flags & Access.NATIVE) != 0)
              {
                buf = new StringBuffer();
                buf.append("/*");
                buf.append(linesep);
                buf.append(" * Class:     ");
                buf.append(className.replace('.', '_'));
                buf.append(linesep);
                buf.append(" * Method:    ");
                String methodname = getUnicodeName(method.getName());
                methodname = escapeUnderscore(methodname);
                methodname = escapeUnicode(methodname);
                buf.append(methodname);
                buf.append(linesep);
                buf.append(" * Signature: ");
                buf.append(method.getSignature());
                buf.append(linesep);
                buf.append(" */");
                buf.append(linesep);
                buf.append("JNIEXPORT ");
                String returnval = getJNIType(method.getReturnType().getName());
                buf.append(returnval);
                buf.append(" JNICALL ");
                String methodname2 = getJNIMethodName(classType, method);
                methodname2 = escapeUnicodeInner(methodname2);
                buf.append(methodname2);
                buf.append(linesep);
                writer.write(buf.toString());
                // begin printing JNI arguments
                buf = new StringBuffer();
                buf.append("  (JNIEnv *, ");
                if ((flags & Access.STATIC) != 0)
                  buf.append("jclass");
                else
                  buf.append("jobject");
                // if there are arguments, include those
                Type[] paramTypes = method.getParameterTypes();
                if (paramTypes.length > 0)
                  buf.append(", ");
                for (int i = 0; i < paramTypes.length; i++)
                  {
                    String jniname = getJNIType(paramTypes[i].getName());
                    if (buf.length() + jniname.length() > 76)
                      {
                        buf.append(linesep);
                        writer.write(buf.toString());
                        buf = new StringBuffer();
                        buf.append("   ");
                      }
                    buf.append(jniname);
                    if ((i + 1) < paramTypes.length)
                      buf.append(", ");
                  }
                buf.append(");");
                buf.append(linesep);
                buf.append(linesep);
                writer.write(buf.toString());
              }
            method = method.getNext();
          }
        buf = new StringBuffer();
        buf.append("#ifdef __cplusplus");
        buf.append(linesep);
        buf.append("}");
        buf.append(linesep);
        buf.append("#endif");
        buf.append(linesep);
        buf.append("#endif");
        buf.append(linesep);
        writer.write(buf.toString());
        writer.close();
        raf.close();
      }
    catch (IOException ioe)
      {
        System.err
          .println("Error: Can't recover from an I/O error with the following message: "
                   + ioe.getMessage());
        return;
      }
  }
}