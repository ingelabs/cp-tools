/* gnu.classpath.tools.JavapMain
   Copyright (C) 2001 Free Software Foundation, Inc.

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

package gnu.classpath.tools;

public class JavapMain
{
  public static void main(String argv[])
  {
    Javaph p = parseArguments(argv);
    int ret = p.exec();
    System.exit(ret);
  }

  /**
   * Parses the arguments to determine what this program
   * should do.
   */
  private static Javaph parseArguments(String s[])
  {
    Javaph p = new Javaph();

    if (s.length == 0)
      usage();

    p.setJavap(true);

    boolean processArguments = true;
    int i = 0;
    while (processArguments)
    {
      if (s[i].equals("-c"))
        p.setDisassemble(true);
      else if (s[i].equals("-classpath"))
        p.setClasspath(s[++i]);
      else if (s[i].equals("-help"))
        usage();
      else if (s[i].equals("-l"))
      {
        p.setPrintLineNumbers(true);
        p.setPrintLocalVariables(true);
      }
      else if (s[i].equals("-public"))
        p.setShowPublic(true);
      else if (s[i].equals("-protected"))
        p.setShowProtected(true);
      else if (s[i].equals("-private"))
        p.setShowPrivate(true);
      else if (s[i].equals("-s"))
        p.setPrintSignatures(true);
      else if (s[i].equals("-verbose"))
      {
        p.setPrintStackSize(true);
        p.setPrintNumberLocals(true);
        p.setPrintMethodArguments(true);
      }
      else if (s[i].startsWith("-"))
      {
        System.err.println("Invalid flag: " + s[i]);
        usage();
      }
      else
        break;

      i++;
    }

    String[] c = new String[s.length - i];
    if (c.length == 0)
      usage();

    for (int j = i; j < s.length; j++)
      c[j - i] = s[j];

    p.setClasses(c);

    return p;
  }

  /**
   * Prints generic usage message to System.out.
   */
  private static void usage()
  {
    System.out.println("Usage: javap [OPTION]... [CLASS]...");
    System.out.println("Provide information about the given classes.");
    System.out.println("");
    System.out.println("   -c                        Disassemble the code");
    System.out.println("   -classpath PATH           Specify where to find user class files");
    System.out.println("   -help                     Print this usage message");
    System.out.println("   -l                        Print line number and local variable tables");
    System.out.println("   -public                   Show only public classes and members");
    System.out.println(
      "   -protected                Show public and protected classes and members");
    System.out.println("   -private                  Show all classes and members");
    System.out.println("   -s                        Print internal type signatures");
    System.out.println(
      "   -verbose                  Print stack size, number of locals and args for methods");
    System.exit(0);
  }
}
