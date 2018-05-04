Introduction
===========================

This project is a continuation of Project 1.
It provides the following improvements:

* Servers can join the network after some users have registered.
  New servers will be updated with the existing registered user info.
* Servers can crash at any time.
  The remaining server group(s) will continue working normally.
* Network partition may happen.
  Availability is guaranteed when partition happens.
  Consistency will eventually be reached when broken network connections are fixed.
  
  
Protocols
===============

The protocols are based on Project 1, with the following differences.

Added
-------

### SYNC_USER

Sent from one server to another server.

Example:
```json
{
  "command": "SYNC_USER",
  "users": {"user1": "secret1", "user2": "secret2"}
}
```

The receiver will

* Update its local storage of registered users.
  Conflicting users will be deleted.
  Clients logged in with conflicting users will be disconnected.
* Forward this message to its downstream servers. 

### ACTIVITY_RETRIEVE

Sent from one server to another server.

Example:
```json
{
  "command": "ACTIVITY_RETRIEVE",
  "after": "foobar"
}
```

The receiver will check its cached ACTIVITY_BROADCAST message history, and
reply with a BUNDLE message which includes all
ACTIVITY_BROADCAST messages _after_ the given message id
(or all ACTIVITY_BROADCAST messages in the cache if the given id is not found).

### BUNDLE

Sent from one server to another server.
This is a bundled message containing a bunch JSON objects,
each of which is a valid message.
It is extremely useful when the sender wants the receiver to process
a transaction that includes several messages in an isolated way,
especially in a high concurrency condition.

Example:
```json
{
  "command": "BUNDLE",
  "messages": [
    {"command": "AUTHENTICATE", "secret":"foobar"},
    {"command": "ACTIVITY_RETRIEVE", "after":"secret2"},
    {"command": "SYNC_USER", "users": {"u1":"s1"}}
  ]
}
```

The receiver will extract all messages in it
and perform actions accordingly.

### NEW_USER

Sent from one server to all the other servers in the network,
after a client registers a new user successfully.

Example:
```json
{
  "command": "NEW_USER",
  "username": "foo",
  "secret": "bar"
}
```

The receiver will

* Keep a record of the username secret pair in its local storage if the username is not known,
  or do nothing if the username is known already with the same secret.
  Then forward this message to its downstream servers. 
* Broadcast a USER_CONFLICT message if the username is known already
  with a different secret. Then delete the corresponding user info.

### USER_CONFLICT

Broadcast by a server when it receives a NEW_USER message which contains a username
known in its local storage already with a different secret.

Example:
```json
{
  "command": "USER_CONFLICT",
  "username": "foo"
}
```

The receiver will:

* Delete the corresponding user if the username exists in its local storage.
* Forward this message to its downstream servers.

_For all protocols above, the receiver will reply with INVALID_MESSAGE
if the sender is not authenticated or if the message is incorrect anyway._

Modified
---------

### SERVER_ANNOUNCE

Same as Project 1, but

* No server id field in the message.
* A server will now send its SERVER_ANNOUNCE messages only to its direct neighbours,
  instead of all servers in the network.
* The announcement is not made periodically, but made when a new server connection is
  established or the client load changes.

```json
{
  "command" : "SERVER_ANNOUNCE",
  "load" : 5,
  "hostname" : "128.250.13.46",
  "port" : 3570
}
```

### ACTIVITY_BROADCAST

Same as Project 1, except each activity broadcast message now has a unique id field.

```json
{
  "command": "ACTIVITY_BROADCAST",
  "activity": {"authenticated_user": "user1"},
  "id": "foobar"
}
```

### AUTHENTICATE

Same as Project 1, except the receiver will reply with a SYNC_USER if authentication
succeeds and there are some users registered already.

Deleted
----------

Lock related protocols (LOCK_REQUEST, LOCK_ALLOWED, and LOCK_DENIED) have been
abandoned.

User registration
=================

Since the registered user lists in different servers are consistent most of the time,
there is no need to use the complicated lock request mechanism.
When a client wants to register a new user, the server will only check its local storage.
If successful, the server will keep a record of the username secret pair,
and broadcast a NEW_USER message to keep the other servers up-to-date.

A USER_CONFLICT message will be broadcast if a server receivers a NEW_USER message
with a username already known with a different secret.
If two or more clients in the network try to register the same username
with different secrets at the same time, the conflicting user will be removed and
those clients that have already logged in with that username will be disconnected,
thanks to the USER_CONFLICT protocol.
Hence the local storage of the servers remains consistent.

Load balancing
================

The redirection mechanism is the same as Project 1.

However, SERVER_ANNOUNCE is now shared locally rather than globally.
Hence one redirection can only take a client to a server's neighbour,
and it could take several steps for a client to get accepted finally.

CAP solution
====================

Availability is instantly guaranteed when one or more servers crash or
network partition happens,
since the system does not rely on the correctness of global information (such as the global
SERVER_ANNOUNCE or LOCK_ALLOWED in Project 1).
The information of local neighbours is sufficient for the system to operate.
Activity broadcast, registration, and redirection still function in each sub-network as expected.

Partition recovery
--------------------

When a connection between two servers is broken,
the child node (if you view the initial server as the root of the tree)
is responsible for reestablishing the connection.

For a broken connection, it's difficult to tell whether it's a network partition or
the parent node is down. As the partition is transient, the child node will try
reconnecting to its parent node periodically until a preset timeout (e.g. 2 hours).

If the connection is fixed, there are two things the two parties need to exchange and
synchronise: activity messages and registered users.

Once the connection breaks, the child server will make a note of the message ID of the last
ACTIVITY_BROADCAST. As soon as the connection is fixed, the initiator, i.e. the child node,
will send a BUNDLE message which includes:

1. AUTHENTICATE. (The other side will reply with SYNC_USER.)
1. ACTIVITY_RETRIEVE. (The other side will reply with all activities missed.)
1. SYNC_USER.
1. All ACTIVITY_BROADCAST messages after the recorded ID in the cache.

(SYNC_USER could be omitted if there's no registered user yet.)

The parent node does not care whether it's a new connection or a fixed connection.
It simply responds to each message as usual.

Consistency is now achieved eventually.

Defending the design
====================

Registration
-----------

In Project 1 LOCK_DENIED almost never occurs,
because the local storage of different servers is always consistent
when there are not concurrent requests.

Hence there's no need to broadcast a LOCK_REQUEST to ask for
others' approvals. Checking one's own local storage is adequate for registering
a user. This is exactly what we do in Project 2. NEW_USER protocol is utilised to
update the local storage of other servers.

In Project 1, LOCK_DENIED could only arise when two clients try to register the same
username. But using USER_CONFLICT is enough to handle this situation, and it is
unnecessary to introduce a whole O(n^2) request / allow / deny mechanism.

Announcement
------------

Making announcement in a fixed time interval is not a good idea. It's going to be
too infrequent under high load, while too frequent if idle.
So a better approach is to make dynamic announcement upon new connection establishment or
server load variation.

Previously the announcement was broadcast in the entire network. It has some disadvantages:

* Poor scalability. The server group will be jammed with all SERVER_ANNOUNCE by itself
  when the network becomes large due to the O(n^2) complexity.
* Not up-to-date. A server can only know if its neighbour is on in real time.
  If one server who has made an announcement earlier dies or network partition
  happens, not all of the servers will notice that. As a result, a client might get redirected
  to an unreachable server.

Redirection
----------

Though redirection might take more than one step now, it's still acceptable because:

* It's a safer and more reliable mechanism.
  A client will not be redirected to an unavailable server
  or a server in another sub-network
  if network partition happens, or
  if some server is down and the original network is separated.
* The extreme conditions are rare.
* Otherwise we would need to share SERVER_ANNOUNCE globally,
  which would produce even more messages.

Concurrency
=============

The BUNDLE protocol provides the system with the isolation property in concurrency control,
which protects the business logic under high concurrency situations.
A BUNDLE message will "lock" the receiver, and make sure the included messages are
processed sequentially without being interrupted by other messages.

Under high concurrency conditions, two extreme cases could happen,
and they are both successfully handled in our protocol.

(a)
If two clients try to register the same username with different secrets at the same time,
neither will succeed due to the USER_CONFLICT protocol.

(b)
If a broken connection is fixed and the two sides are synchronising registered users,
but meanwhile another server on one side is registering a user that already exists on
the other side, the USER_CONFLICT mechanism will also take effect and the user is removed
from the entire system. 

Scalability
=============

The system is very scalable as the operations are all in O(n) time now.

Memo
====

* Don't forget to disconnect clients with conflict servers when receiving USER_CONFLICT or SYNC_USER
* When partition is fixed, the initiator sends AUTHENTICATE, ACTIVITY_BROADCAST,
  ACTIVITY_RETRIEVE, SYNC_USER, SERVER_ANNOUNCE,
  and the acceptor sends SYNC_USER, SERVER_ANNOUNCE
* When processing bundle message, check connection closed status