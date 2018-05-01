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

The receiver will

* update its local storage of registered users,
  and forward this message to its downstream servers. 
* Reply with a non-overriding SYNC_USER message containing its own unique user info
  if `override` is true and the receiver owns some users not included in this message.

Example:
```json
{
    "command": "SYNC_USER",
    "users": {"user1": "secret1", "user2": "secret2"},
    "override": true
}
```

### ACTIVITY_RETRIEVE

Sent from one server to another

```json
{
    "command": "ACTIVITY_RETRIEVE",
    "after": "foobar",
    "needReply": true
}
```

### NEW_CLIENT

```json
{
    "command": "NEW_CLIENT"
}
```

### CLIENT_DISCONNECT

```json
{
    "command": "CLIENT_DISCONNECT"
}
```

Modified
---------

### ACTIVITY_BROADCAST

Same as Project 1, except each activity broadcast message will have a unique id field.

```json
{
    "command": "ACTIVITY_BROADCAST",
    "activity": {},
    "id": "foobar"
}
```

### AUTHENTICATE

Same as Project 1, except the receiver will reply with an overriding 
SYNC_USER message if successful.

### LOCK_REQUEST

Same as Project 1, except the receiver will

* Reply with a LOCK_DENIED if the username is already known to the server, or
  any of its downstream neighbours sends it LOCK_DENIED for this username secret pair.
* Reply with a LOCK_ALLOWED if the username is not known to the server, and
  all of its downstream neighbours send it LOCK_ALLOWED for this username secret pair.
* Record this username and secret pair in its local storage if the username is not known.

_For all protocols above, the receiver will reply with INVALID_MESSAGE
if the sender is not authenticated or if the message is incorrect anyway._

Deleted
----------

SERVER_ANNOUNCE is abandoned.

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
