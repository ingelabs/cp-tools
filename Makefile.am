## Process this file with automake to produce Makefile.in.

# Allow users to pass site-specific flags to autoreconf via an env var. 
ACLOCAL_AMFLAGS = -I m4 ${LOCAL_AUTORECONF_FLAGS}

BYTECODE_JAR=src/jars/bytecode.jar
DISTCLEANFILES = $(BYTECODE_JAR) 

if USE_GNUBYTECODE
AM_GCJFLAGS = --classpath=$(CLASSPATH) -fassume-compiled -I$(srcdir)/src -I$(XML_JAR) -I$(BYTECODE_JAR) -I.
else
AM_GCJFLAGS = --classpath=$(CLASSPATH) -fassume-compiled -I$(srcdir)/src -I$(XML_JAR) -I.
endif

MY_JAVAC_FLAGS = -classpath $(CLASSPATH):$(srcdir)/src -g -source 1.5 -target 1.5 $(JAVAC_FLAGS)

LIBGCJCOMPILE = $(LIBTOOL) --tag=GCJ --mode=compile $(GCJ) $(AM_GCJFLAGS) $(GCJFLAGS) -c -MD -MT $@
GCJCOMPILE = $(LIBGCJCOMPILE) -MF $(@:.o=.d)
LTGCJCOMPILE = $(LIBGCJCOMPILE) -MF $(@:.lo=.d)

if ENABLE_GCJ_BINARY
bin_PROGRAMS = localegen supplementgen
lib_LTLIBRARIES = libcpgenerators.la 
if USE_GNUBYTECODE
bin_PROGRAMS += gjavap gjavah
lib_LTLIBRARIES += libcptools.la libbytecode.la 
libbytecode_la_LINK = $(GCJLINK) 
nodist_libbytecode_la_SOURCES = $(BYTECODE_JAR)
endif
else
bin_SCRIPTS = bin/localegen bin/supplementgen
data_JARS = cpgenerators-$(VERSION).jar
BUILT_SOURCES = $(data_JARS)
if USE_GNUBYTECODE
bin_SCRIPTS += bin/gjavap bin/gjavah
data_JARS += cptools-$(VERSION).jar
endif
endif

if ENABLE_GCJ_BINARY
if USE_GNUBYTECODE
gjavap_SOURCES =
gjavap_LINK = $(GCJLINK)
gjavap_LDADD = libcptools.la 
gjavap_LDADD += libbytecode.la
gjavap_LDFLAGS = --main=gnu.classpath.tools.javap.JavapMain \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

gjavah_SOURCES =
gjavah_LINK = $(GCJLINK)
gjavah_LDADD = libcptools.la 
gjavah_LDADD += libbytecode.la
gjavah_LDFLAGS = --main=gnu.classpath.tools.javah.JavahMain \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never
endif

localegen_SOURCES =
localegen_LINK = $(GCJLINK)
localegen_LDADD = libcpgenerators.la 
localegen_LDFLAGS = --main=gnu.localegen.Main \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

supplementgen_SOURCES =
supplementgen_LINK = $(GCJLINK)
supplementgen_LDADD = libcpgenerators.la 
supplementgen_LDFLAGS = --main=gnu.supplementgen.Main \
	-Dgnu.gcj.runtime.VMClassLoader.library_control=never

else

gjavap: $(srcdir)/bin/gjavap.in
gjavah: $(srcdir)/bin/gjavah.in
localegen: $(srcdir)/bin/localegen.in
supplementgen: $(srcdir)/bin/supplementgen.in

endif # if ENABLE_GCJ_BINARY

if USE_GNUBYTECODE
cptools_jar_SOURCES = \
	src/gnu/classpath/tools/javah/JavahMain.java \
	src/gnu/classpath/tools/javah/Javah.java \
	src/gnu/classpath/tools/javap/JavapMain.java \
	src/gnu/classpath/tools/javap/Javap.java \
	src/gnu/classpath/tools/Util.java \
	src/gnu/classpath/tools/javah/GnuByteCodeJavah.java \
	src/gnu/classpath/tools/javap/GnuByteCodeJavap.java

if ENABLE_GCJ_BINARY
libcptools_la_SOURCES = $(cptools_jar_SOURCES)
else

cptools_jar_CLASSES = $(patsubst src/%.java,classes/%.class,$(cptools_jar_SOURCES))

cptools-$(VERSION).jar: createdirectories $(cptools_jar_CLASSES)
	$(JAR) cf $@ -C classes gnu/classpath

#gjdoc.mf:
#	echo 'Main-Class: gnu.classpath.tools.gjdoc.Main' > gjdoc.mf

## We don't use $(GCJFLAGS) in this rule, because it might contain '-O',
## and that doesn't work that great when generating bytecode.
##
## FIXME change this to be user-configurable, so '-g' can be disabled.

#$(cptools_jar_CLASSES): $(lib_gnu_classpath_tools_gjdoc_la_GENSOURCES) 

$(cptools_jar_CLASSES): classes/gnu/%.class: $(srcdir)/src/gnu/%.java
	$(JAVAC) $(MY_JAVAC_FLAGS) -d classes $<

endif
endif

localegen_java_files = \
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
	src/gnu/localegen/Main.java

supplementgen_java_files = \
	src/gnu/supplementgen/Main.java

cpgenerators_jar_SOURCES = $(localegen_java_files) $(supplementgen_java_files)

if ENABLE_GCJ_BINARY
libcpgenerators_la_SOURCES = $(cpgenerators_jar_SOURCES)
else
cpgenerators_jar_CLASSES = $(patsubst src/%.java,classes/%.class,$(cpgenerators_jar_SOURCES))

cpgenerators-$(VERSION).jar: createdirectories $(cpgenerators_jar_CLASSES)
	(cd classes; \
	$(JAR) cf ../$@ gnu/ldml gnu/localegen gnu/supplementgen; \
	cd ..)

$(cpgenerators_jar_CLASSES): classes/gnu/%.class: $(srcdir)/src/gnu/%.java
	$(JAVAC) $(MY_JAVAC_FLAGS) -d classes $<
endif

createdirectories:
	mkdir -p classes

clean-local:
	rm -rf classes $(data_JARS)

if !ENABLE_GCJ_BINARY
install-data-hook:
	$(INSTALL) -d $(datadir)/cp-tools
	$(INSTALL) --mode 644 $(data_JARS) $(datadir)/cp-tools
	$(LN_S) -f $(datadir)/cp-tools/cpgenerators-$(VERSION).jar $(datadir)/cp-tools/cpgenerators.jar
	if [ -e cptools-$(VERSION).jar ]; then \
	  $(LN_S) -f $(datadir)/cp-tools/cptools-$(VERSION).jar $(datadir)/cp-tools/cptools.jar; \
	fi
endif

test: tests/%.class $(srcdir)/src/test/%.java
	$(JAVAC) -g -d tests $<
