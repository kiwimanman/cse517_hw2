cse517_hw2
==========

```
cd $PROJECT_ROOT/src
mkdir classes
javac -d classes */*/*/*.java */*/*/*/*.java
java -cp classes edu.berkeley.nlp.Test
java -cp classes edu.berkeley.nlp.assignments.POSTaggerTester  -model hmm -decoder veterbi -test test -verbose
```
