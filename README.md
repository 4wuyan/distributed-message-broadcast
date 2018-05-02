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

* Keep a record of the username secret pair in its local storage,
  if the username is not known.
* Reply with a USER_CONFLICT message if the username is known already
  with a different secret.
* Do nothing if the username is known already with the same secret.
* Forward this message to its downstream servers. 

### USER_CONFLICT

Example:
```json
{
    "command": "USER_CONFLICT",
    "username": "foo",
    "secret": "bar"
}
```

_For all protocols above, the receiver will reply with INVALID_MESSAGE
if the sender is not authenticated or if the message is incorrect anyway._

Deleted
----------

Lock related protocols (LOCK_REQUEST, LOCK_ALLOWED, and LOCK_DENIED) have been
abandoned.

User registration
=================

_To be continued_

Load balancing
================

_To be continued_

Partition recovery
==================

_To be continued_

Activity message synchronisation
---------------------------------

_To be continued_

Registered user info synchronisation
-----------------------------------

_To be continued_


Memo
====

* Don't forget to disconnect clients with conflict servers when receiving USER_CONFLICT or SYNC_USER
* When partition is fixed, the initiator sends AUTHENTICATE, ACTIVITY_RETRIEVE, SYNC_USER, SERVER_ANNOUNCE,
  and the acceptor sends SYNC_USER, ACTIVITY_RETRIEVE, SERVER_ANNOUNCE