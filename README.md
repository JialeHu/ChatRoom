# ChatRoom
Implementations of chat room server and client that enable sending public or private messages and user management via socket.
#### Server
- Connects to Database (IBM DB2) via JDBC SQL
- Serializes Data using JSON
- Utilizes Thread pool for communication with clients
#### Client
- GUIs with Swing
- Message Queue using Monitor
- Supports user info settings
#### Dependency
- JDK 13
- [org.json release 20200518](https://github.com/stleary/JSON-java)
- [org.json.simple release 1_1_1](https://github.com/fangyidong/json-simple)
## Quick Link
- [Server Source Code](/ChatRoom-Server/src/my/chatroom/server)
- [Client Source Code](/ChatRoom-Client/src/my/chatroom/client)
- [Packaged JAR](/JAR)
## Screenshots
<h3> Client <h3/>
<img src="/Screenshots/client.png" width="600"/>
<h3> Client Settings <h3/>
<img src="/Screenshots/settings.png" width="300"/>
<h3> Client Login <h3/>
<img src="/Screenshots/login.png" width="300"/>
<h3> Client SignUp <h3/>
<img src="/Screenshots/signup.png" width="300"/>
<h3> Server Console <h3/>
<img src="/Screenshots/server.png" width="600"/>
