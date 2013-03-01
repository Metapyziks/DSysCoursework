SRC = \
	src/IDatabaseConnection.java \
	src/Host.java \
	src/Endpoint.java \
	src/Server.java \
	src/Client.java

CLASS = \
	bin/IDatabaseConnection.class \
	bin/Host.class \
	bin/Endpoint.class \
	bin/Server.class \
	bin/Client.class

$(CLASS): $(SRC)
	mkdir -p bin/
	javac -classpath .:bin -d bin $(SRC)
