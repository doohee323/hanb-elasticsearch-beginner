hanb-elasticsearch-beginner
===========================

1) making server
You can make a clustered server which contains 3 folders, node1, node2, node3.
Each of node has a configuration file like "elasticsearch.yml".

node.name: "node1"
transport.tcp.port: 9300
http.port: 9200

node.name: "node2"
transport.tcp.port: 9301
http.port: 9201

node.name: "node2"
transport.tcp.port: 9302
http.port: 9202

And you can run the each server with this command.

./elasticsearch/node1> ./start.sh
./elasticsearch/node2> ./start.sh
./elasticsearch/node3> ./start.sh

- health check:
http://localhost:9200/_nodes?pretty=true

-------------------------------------------------

2) Data indexing and Query
 2-1. Create Index
 	CreateIndexTest.java
 2-2. Indexing
 	IndexingTest.java
 2-3. Query
	QueryTest.java

