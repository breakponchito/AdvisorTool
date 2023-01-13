# AdvisorTool
This initial project to make the advisor tool

To use please follow next steps 

download project

```
git clone https://github.com/breakponchito/AdvisorTool.git
```

move to the  master branch

```
git checkout master 
```

build the project:

```
mvn clean install
```

Execute Command under a project that you want to advise for jakarta 10

```
mvn fish.payara.advisor:advisor-maven-plugin:1.0-SNAPSHOT:advisor-tool
```

if a pattern matchs you will see something like the following:

```
[INFO] Showing Advices
[INFO] ********
 Jakarta Authorization 2.1
 Issue # 105
 jakarta.security.jacc.PolicyContext.getContext(String) was changed from
 public static Object getContext(String key) throws PolicyContextException {
 to
 public static <T> T getContext(String key) throws PolicyContextException {
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```
