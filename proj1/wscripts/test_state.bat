@echo off
set peer=%1
if [%1]==[] set peer="ap"
java -cp build main.g06.TestApp %peer% STATE