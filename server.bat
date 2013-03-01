@ECHO off

cd bin
java Server "../hosts.txt" "%1" "%2"
cd ..
