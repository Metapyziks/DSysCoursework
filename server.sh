#!/bin/sh

cd bin
java Server "../hosts.txt" "$1" "$2"
cd ..
