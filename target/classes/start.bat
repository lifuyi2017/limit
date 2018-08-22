@echo off

set base=%~dp0

set lib=%base%\lib

set class_path=%base%;

java -Djava.ext.dirs=%lib% -classpath %class_path% com.cigc.limit.Program
@pause