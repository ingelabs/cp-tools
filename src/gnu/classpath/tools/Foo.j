/* Foo.java
 * Demonstrates problem with calling native method in static initializer
 */
public class Foo 
{
  private static native void bar();
  static { bar(); }
  static void foo(int a) { bar(); }
  void foo() { bar(); }
} 
