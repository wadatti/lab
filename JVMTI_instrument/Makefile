JAVA_HOME = /Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home
CFLAGS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin

run: Test.class agent.so
	java -agentpath:./agent.so Test

Test.class: Test.java
	javac Test.java

agent.so: agent.c
	clang -shared -o agent.so agent.c $(CFLAGS)

clean:
	rm -f *.so *.class
