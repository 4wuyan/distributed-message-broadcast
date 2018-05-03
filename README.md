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
    "activity": {},
    "id": "foobar"
}
```

### AUTHENTICATE

Same as Project 1, except the receiver will reply with a SYNC_USER message if successful.

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

* Reply with all the ACTIVITY_BROADCAST messages _after_ the given message id,
  if the given id is found in the history.
* Reply with all the ACTIVITY_BROADCAST messages in the cache,
  if the given id is not found.

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
* Reply with a USER_CONFLICT message if the username is known already
  with a different secret. Then delete the corresponding user info.

### USER_CONFLICT

Sent from Server A to Server B when B sends A a NEW_USER message with a
username known to A already with a different secret.

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
and broadcast a NEW_USER message to keep the other servers updated.

A USER_CONFLICT message will be initiated if a server receivers a NEW_USER message
with a username already known with a different secret.
If two or more clients in the network try to register the same username
with different secrets at the same time, every server in the network will receive
at least one USER_CONFLICT message finally. The conflicting user will be removed and
those clients that have already logged in with that username will be disconnected.
Hence the local storage of the servers remains consistent.

Load balancing
================

The redirection mechanism is the same as Project 1.

However, SERVER_ANNOUNCE is now shared locally rather than globally.
Hence one redirection can only take a client to a server's neighbour,
and it could take several steps for a client to get accepted finally.

Partition recovery
==================

When a connection between two servers is broken,
the child node (if you view the initial server as the root of the tree)
is responsible for reestablishing the connection.

For a broken connection, it's difficult to tell whether it's a network partition or
the parent node is down. As the partition is transient, the child node will try
reconnecting to its parent node periodically until a preset timeout (e.g. 2 hours).

If the connection is fixed, there are two things the two parties need to exchange and
synchronise: activity messages and registered users.

Activity message synchronisation
---------------------------------

Once the connection breaks, the child server will make a note of the message ID of the last
ACTIVITY_BROADCAST. Once the connection is fixed, after a routine AUTHENTICATE,
the initiator, i.e. the child node, will:

* Send all ACTIVITY_BROADCAST messages after the recorded ID in the cache to the other side.
* Send an ACTIVITY_RETRIEVE to retrieve all the ACTIVITY_BROADCAST messages after the recorded
  ID from the other side.
* Send SYNC_USER to the other side.

The parent node does not care whether it's a new connection or fixed connection.
It simply responds to each message in the normal way.

Registered user info synchronisation
-----------------------------------

_To be continued_

Defending the design
====================

blah blah

Concurrency
-----------

Scalability
-----------

Memo
====

* Don't forget to disconnect clients with conflict servers when receiving USER_CONFLICT or SYNC_USER
* When partition is fixed, the initiator sends AUTHENTICATE, ACTIVITY_BROADCASTs,
  ACTIVITY_RETRIEVE, SYNC_USER, SERVER_ANNOUNCE,
  and the acceptor sends SYNC_USER, SERVER_ANNOUNCE