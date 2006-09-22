## Process this file with automake to produce Makefile.in.

BYTECODE_JAR=src/jars/bytecode.jar
ASM_JAR=src/jars/asm.jar
DISTCLEANFILES = $(BYTECODE_JAR) $(ASM_JAR)

AM_GCJFLAGS = --classpath=$(CLASSPATH) -fassume-compiled -I$(srcdir)/src -I$(XML_JAR) -I$(BYTECODE_JAR) -I$(ASM_JAR) -I.

MY_JAVAC_FLAGS = -classpath $(CLASSPATH):$(srcdir)/src -g $(JAVAC_FLAGS)

LIBGCJCOMPILE = $(LIBTOOL) --tag=GCJ --mode=compile $(GCJ) $(AM_GCJFLAGS) $(GCJFLAGS) -c -MD -MT $@
GCJCOMPILE = $(LIBGCJCOMPILE) -MF $(@:.o=.d)
LTGCJCOMPILE = $(LIBGCJCOMPILE) -MF $(@:.lo=.d)

data_JARS = cptools-$(VERSION).jar

BUILT_SOURCES = $(data_JARS)

if ENABLE_GCJ_BINARY
bin_PROGRAMS = javap javah localegen currencygen
lib_LTLIBRARIES = libcptools.la 
lib_LTLIBRARIES += libasm.la
if USE_GNUBYTECODE
lib_LTLIBRARIES += libbytecode.la
libbytecode_la_LINK = $(GCJLINK) 
nodist_libbytecode_la_SOURCES = $(BYTECODE_JAR)
endif
else
bin_SCRIPTS = javap javah localegen currencygen
endif

if ENABLE_GCJ_BINARY
javap_SOURCES =
javap_LINK = $(GCJLINK)
javap_LDADD = libcptools.la 
if USE_GNUBYTECODE
javap_LDADD += libbytecode.la
endif
javap_LDFLAGS = --main=gnu.classpath.tools.javap.JavapMain \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

javah_SOURCES =
javah_LINK = $(GCJLINK)
javah_LDADD = libcptools.la 
if USE_GNUBYTECODE
javah_LDADD += libbytecode.la
endif
javah_LDFLAGS = --main=gnu.classpath.tools.javah.JavahMain \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

localegen_SOURCES = \
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
        src/gnu/localegen/PropertiesGenerator.java \
	src/gnu/localegen/CollationInterpreter.java \
	src/gnu/localegen/JavaGenerator.java \
	src/gnu/localegen/Main.java
localegen_LDFLAGS = --main=gnu.localegen.Main \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

currencygen_SOURCES = \
	src/gnu/currencygen/Main.java
currencygen_LDFLAGS = --main=gnu.currencygen.Main

else
javap: $(srcdir)/bin/javap
	cp $< $@
	chmod +x $@

javah: $(srcdir)/bin/javah
	cp $< $@
	chmod +x $@

localegen: $(srcdir)/bin/localegen
	cp $< $@
	chmod +x $@

currencygen: $(srcdir)/bin/currencygen
	cp $< $@
	chmod +x $@

endif # if ENABLE_GCJ_BINARY

cptools_jar_SOURCES = \
	src/gnu/classpath/tools/javah/JavahMain.java \
	src/gnu/classpath/tools/javah/Javah.java \
	src/gnu/classpath/tools/javah/GnuByteCodeJavah.java \
	src/gnu/classpath/tools/javap/JavapMain.java \
	src/gnu/classpath/tools/javap/Javap.java \
	src/gnu/classpath/tools/javap/GnuByteCodeJavap.java \
	src/gnu/classpath/tools/Util.java

if ENABLE_GCJ_BINARY
libcptools_la_SOURCES = $(cptools_jar_SOURCES)
endif

cptools_jar_CLASSES = $(patsubst src/%.java,classes/%.class,$(cptools_jar_SOURCES))

cptools-$(VERSION).jar: createdirectories $(cptools_jar_CLASSES)
	$(JAR) cf $@ -C classes gnu

createdirectories:
	mkdir -p classes

#gjdoc.mf:
#	echo 'Main-Class: gnu.classpath.tools.gjdoc.Main' > gjdoc.mf

## We don't use $(GCJFLAGS) in this rule, because it might contain '-O',
## and that doesn't work that great when generating bytecode.
##
## FIXME change this to be user-configurable, so '-g' can be disabled.

#$(cptools_jar_CLASSES): $(lib_gnu_classpath_tools_gjdoc_la_GENSOURCES) 

$(cptools_jar_CLASSES): classes/gnu/%.class: $(srcdir)/src/gnu/%.java
	$(JAVAC) $(MY_JAVAC_FLAGS) -d classes $<

clean-local:
	rm -rf classes $(data_JARS)

dist-hook:
	test -d $(distdir)/bin/ || mkdir $(distdir)/bin
	cp $(srcdir)/bin/currencygen $(distdir)/bin/
	cp $(srcdir)/bin/javah $(distdir)/bin/
	cp $(srcdir)/bin/javap $(distdir)/bin/
	cp $(srcdir)/bin/localegen $(distdir)/bin/
	cp $(srcdir)/bin/native2ascii $(distdir)/bin/

install-data-hook:
	$(INSTALL) -d $(datadir)/java
	$(INSTALL) --mode 644 $(data_JARS) $(datadir)/java
	$(LN_S) -f $(datadir)/java/cptools-$(VERSION).jar $(datadir)/java/cptools.jar

test: tests/%.class $(srcdir)/src/test/%.java
	$(JAVAC) -g -d tests $<
