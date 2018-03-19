# Publish-Subscribe System Demo
#### Go to the directory:
#### $cd FinalProjectDeliverable/
#### Compile:
#### $javac src/pssystem/*.java
#### Start 3 servers and 2 clients (in 5 separate terminals):
#### $java -cp ./src pssystem.Server 2222
#### $java -cp ./src pssystem.Server 2223
#### $java -cp ./src pssystem.Server 2224
#### $java -cp ./src pssystem.Client 8888
#### $java -cp ./src pssystem.Client 8889
#### Subscribe:
#### let client 8888 subscribes to topic #0 and #1;
#### let client 8889 subscribe to topic #0 and #2:
#### In client 8888 terminal: $0 1
#### In client 8889 terminal: $0 2
#### Publish message in certain topic from server(leader, should be 2222 initially) $0:Hi_From_Topic_0
#### $1:Hi_From_Topic_1
#### $2:Hi_From_Topic_2
#### After this, can see message 0 & 1 appear on client 8888, and 0 & 2 on client 8889. Shut down leader to test leader election (it will takes around 36 sec)
#### After around 36 sec should see 2224 as the new leader.
#### Restart client1 to test continuous subscription
#### send from new leader(should be 2224 after leader election) $0:Hi_From_Topic_0
#### $1:Hi_From_Topic_1
#### $2:Hi_From_Topic_2
#### After this, can see message 0 & 1 appear on client 8888, and 0 & 2 on client 8889.

### Please visit this youtube link https://youtu.be/46XvT23DMho to see our demo. Enjoy!
