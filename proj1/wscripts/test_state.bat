@echo off
set peer=%1
if [%1]==[] set peer="ap"
java -cp src/build main.g06.TestApp %peer% STATE