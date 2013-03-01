SRC = \
	src/StructuredFileReader.java \
	src/Department.java \
	src/Student.java \
	src/Query.java \
	src/IDatabaseConnection.java \
	src/Host.java \
	src/Endpoint.java \
	src/Server.java \
	src/Client.java

CLASS = \
	bin/StructuredFileReader.class \
	bin/Department.class \
	bin/Student.class \
	bin/Query.class \
	bin/IDatabaseConnection.class \
	bin/Host.class \
	bin/Endpoint.class \
	bin/Server.class \
	bin/Client.class

$(CLASS): $(SRC)
	mkdir -p bin/
	javac -classpath .:bin -d bin $(SRC)
