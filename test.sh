
CP=lib/httpclient-4.1.1.jar:lib/httpclient-cache-4.1.1.jar
CP=$CP:lib/httpcore-4.1.jar:lib/httpcore-nio-4.1.jar:lib/httpmime-4.1.1.jar
CP=$CP:lib/commons-logging-1.1.1.jar
CP=$CP:lib/skrAPI.jar

java -cp ./classes:$CP $*