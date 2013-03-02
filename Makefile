SRC = \
	src/StructuredFileReader.java \
	src/Department.java \
	src/Student.java \
	src/QuerySyntaxException.java \
	src/Query.java \
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
	bin/QuerySyntaxException.class \
	bin/Query.class \
	bin/QueryResponse.class \
	bin/IDatabaseConnection.class \
	bin/Host.class \
	bin/Endpoint.class \
	bin/Server.class \
	bin/Client.class

$(CLASS): $(SRC)
	mkdir -p bin/
	javac -classpath .:bin -d bin $(SRC)

report.pdf: report.tex
	pdflatex report.tex

tests.pdf: tests.tex
	pdflatex tests.tex
