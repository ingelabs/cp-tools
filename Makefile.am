## Process this file with automake to produce Makefile.in.

AM_GCJFLAGS = --classpath=$(CLASSPATH) -fassume-compiled -I$(srcdir)/src -I$(KAWA_JAR) -I.

LIBGCJCOMPILE = $(LIBTOOL) --tag=GCJ --mode=compile $(GCJ) $(AM_GCJFLAGS) $(GCJFLAGS) -c -MD -MT $@
GCJCOMPILE = $(LIBGCJCOMPILE) -MF $(@:.o=.d)
LTGCJCOMPILE = $(LIBGCJCOMPILE) -MF $(@:.lo=.d)

data_JARS = cp-tools-$(VERSION).jar

BUILT_SOURCES = $(data_JARS)

bin_PROGRAMS = javap javah serialver localegen currencygen
lib_LTLIBRARIES = lib-cp-tools.la 

javap_SOURCES =
javap_LINK = $(GCJLINK)
javap_LDADD = -l-cp-tools
javap_LDFLAGS = --main=gnu.classpath.tools.JavapMain \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

javah_SOURCES =
javah_LINK = $(GCJLINK)
javah_LDADD = -l-cp-tools
javah_LDFLAGS = --main=gnu.classpath.tools.JavahMain \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

serialver_SOURCES =
serialver_LINK = $(GCJLINK)
serialver_LDADD = -l-cp-tools
serialver_LDFLAGS = --main=gnu.classpath.tools.Serialver \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

localegen_SOURCES =
localegen_LINK = $(GCJLINK)
localegen_LDADD = -l-cp-tools -l-gnu-xml -lxmlj
localegen_LDFLAGS = --main=gnu.localegen.Main \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

currencygen_SOURCES =
currencygen_LINK = $(GCJLINK)
currencygen_LDADD = -l-cp-tools -l-gnu-xml -lxmlj
currencygen_LDFLAGS = --main=gnu.currencygen.Main \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

lib_cp_tools_la_SOURCES = \
	src/gnu/classpath/tools/JavahMain.java \
	src/gnu/classpath/tools/Javaph.java \
	src/gnu/classpath/tools/JavapMain.java \
        src/gnu/classpath/tools/SerialVer.java \
	src/gnu/classpath/tools/Util.java \
	src/gnu/currencygen/Main.java \
	src/gnu/ldml/AliasElement.java \
	src/gnu/ldml/Analyzer.java \
	src/gnu/ldml/Constants.java \
	src/gnu/ldml/DataElement.java \
	src/gnu/ldml/DetailedListElement.java \
	src/gnu/ldml/Element.java \
	src/gnu/ldml/ExpansionElement.java \
	src/gnu/ldml/ListDataElement.java \
	src/gnu/ldml/OrderedListBaseElement.java \
	src/gnu/ldml/OrderedListElement.java \
	src/gnu/ldml/ParseException.java \
	src/gnu/ldml/Parser.java \
	src/gnu/ldml/ResetElement.java \
	src/gnu/localegen/CollationInterpreter.java \
	src/gnu/localegen/JavaGenerator.java \
	src/gnu/localegen/Main.java

cp_tools_jar_CLASSES = $(patsubst src/%.java,classes/%.class,$(lib_cp_tools_la_SOURCES))

cp-tools-$(VERSION).jar: createdirectories $(cp_tools_jar_CLASSES)
	$(JAR) cf $@ -C classes gnu

createdirectories:
	mkdir -p classes

#gjdoc.mf:
#	echo 'Main-Class: gnu.classpath.tools.gjdoc.Main' > gjdoc.mf

## We don't use $(GCJFLAGS) in this rule, because it might contain '-O',
## and that doesn't work that great when generating bytecode.
##
## FIXME change this to be user-configurable, so '-g' can be disabled.

#$(cp_tools_jar_CLASSES): $(lib_gnu_classpath_tools_gjdoc_la_GENSOURCES) 

$(cp_tools_jar_CLASSES): classes/%.class: $(srcdir)/src/%.java
	$(GCJ) $(AM_GCJFLAGS) -g -C -d classes $<

clean-local:
	rm -rf classes $(data_JARS) antlr.jar

dist-hook:
	test -d $(distdir)/src/ || mkdir $(distdir)/src

install-data-hook:
	$(INSTALL) -d $(datadir)/java
	$(INSTALL) -d $(datadir)/java/ext
	$(INSTALL) --mode 644 $(data_JARS) $(datadir)/java
	$(LN_S) -f $(datadir)/java/cp-tools-$(VERSION).jar $(datadir)/java/cp-tools.jar
