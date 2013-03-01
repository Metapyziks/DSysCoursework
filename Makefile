SRC = \
	src/StructuredFileReader.java \
	src/Department.java \
	src/Student.java \
	src/QueryResponse.java \
	src/IDatabaseConnection.java \
	src/Host.java \
	src/Endpoint.java \
	src/Server.java \
	src/Client.java

CLASS = \
	bin/StructuredFileReader.class \
	bin/Department.class \
	bin/Student.class \
	src/QueryResponse.java \
	bin/IDatabaseConnection.class \
	bin/Host.class \
	bin/Endpoint.class \
	bin/Server.class \
	bin/Client.class

$(CLASS): $(SRC)
	mkdir -p bin/
	javac -classpath .:bin -d bin $(SRC)
