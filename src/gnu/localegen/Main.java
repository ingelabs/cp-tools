package gnu.localegen;

import gnu.ldml.Analyzer;
import java.net.URL;
import java.util.Hashtable;
import java.util.Enumeration;
import gnu.ldml.Element;
import java.io.IOException;

public class Main {


  static void printVersion()
  {
    System.out.println(" This is the LDML to GNU Classpath converter");
    System.out.println("   Copyright (C) 2004 The Free Software Foundation.");
    System.out.println();
  }

  static void printUsage()
  {
    System.out.println(" Usage: [URLs]");
    System.out.println();
    System.out.println("The generator takes only URL on command line. The corresponding files will be retrieved and parsed.");
    System.out.println("The Java source code for GNU Classpath is generated in gnu/java/locale/LocaleInformation_[name]");
    System.out.println("where \"name\" is the proper name of the original XML file (atm), i.e. the base file name.");
    System.exit(1);
  }

  static public void main(String args[]) throws Exception
  {

    printVersion();

    if (args.length == 0)
      {
	System.out.println("Invalid number of arguments.");
	printUsage();
      }

    for (int i = 0; i < args.length; i++)
      {
	URL u;
	Analyzer a;

	try
	  {
	    u = new URL(args[i]);
	    
	    System.out.println("Parsing/Analyzing initial URL " + u);
	    a = new Analyzer(u);
	  }
	catch (IOException e)
	  {
	    System.out.println("It is impossible to grab the requested file (reason="+ e.getMessage() + ")");
	    e.printStackTrace();
	    System.out.println("Exiting.");
	    return;
	  }
	
	System.out.println("Parsed. Generating Java source code for " + a.getParser().getName() + " in gnu.java.locale");
	
	Hashtable flattree = a.flattenTree();
	Enumeration keys = flattree.keys();
	JavaGenerator generator;
	
	generator = new JavaGenerator("gnu.java.locale", a);
	generator.generate(null);
      }
  }

}
