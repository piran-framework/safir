# PIRAN/SAFIR-DARBAAN Specification

* Name: PIRAN/Safir-Darbaan
* Version: 1
* Status: draft
* Editor: Isa Hekmatizadeh esa.hekmat@gmail.com

The Safir-Darbaan specification, is based on
[FLP(Freelance Protocol)](http://rfc.zeromq.org/spec:10/FLP), instead that FLP designed for a 
large number of clients and a few servers, but SAFIR-DARBAAN suppose a few clients and a large 
number of servers. Generally SAFIR-DARBAAN specification governs and defines a brokerless and 
reliable way for a group of nodes consist of CHANNEL and SERVER to talk to each other. 

## License
Copyright (c) 2018 Isa Hekmatizadeh.

This Specification is free software; you can redistribute it and/or modify it under the terms of 
the GNU General Public License as published by the Free Software Foundation; either version 3 of 
the License, or (at your option) any later version.

This Specification is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.  

You should have received a copy of the GNU General Public License along with this program; if 
not, see <http://www.gnu.org/licenses>.

## Language

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", 
"RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in RFC 
2119 (see "[Key words for use in RFCs to Indicate Requirement Levels](http://tools.ietf.org/html/rfc2119)").

## Goals

Unlike FLP, SAFIR-DARBAAN assumes a large set of SERVERs and a few CHANNELs which act as clients.
Servers MAY differ from each other and MAY serve different services. Channels could send a 
request to the specific server and get back the response. 

The goals of SAFIR-DARBAAN are:

- To enable an N-to-N network of different clients and servers, connected to each other in 
peer-to-peer fashion
- To operate without an intermediary broker or devices.
- To enable multi-threaded server and clients implementations.
- To enable server failover and recovery.

## Architecture

In SAIFR-DARBAAN protocol, Servers connect to Channels and Servers can reply to channels that 
have first sent a command.

Nodes RECOMMENDED to find each other by
[PIRAN/RBND](https://github.com/piran-framework/geev/blob/master/RBND-Specification.md), each node MUST
create a Router socket and channels MUST bind it. Router socket of servers SHOULD 
connect to channels socket. and SHOULD start the conversation by introducing server itself to the
channel by an INTR command. 

Channels MAY send REQ command to send a request and servers answer to that command by REP.

Servers MAY change their services, after any change is SHOULD resend an INTR command again to 
notify all channels about the change.

Channels SHOULD check the availability of servers in the time of silence by sending PING command.
and Servers SHOULD answer PING command with PONG command. Any other conversations can assume as 
heartbeat for channels.

If a channel restarted, it can send a RINTR(request for INTR) command to servers. and servers 
SHOULD response by INTR command.

## Request and Response Routing

Both clients and servers MUST use ROUTER (XREP) sockets. From the ØMQ Reference Manual: 

>When receiving messages a ROUTER (XREP) socket shall prepend a message part containing the 
>identity of the originating peer to the message before passing it to the application. When 
>sending messages a ROUTER (XREP) socket shall remove the first part of the message and use it to 
>determine the identity of the peer the message shall be routed to.   

Servers use *transient* sockets, and MUST not set an identity. Channels use *durable* sockets and
MUST set an identity.

Channel identities are their *public endpoints*. This is the address string that servers will use
to connect to the server, e.g. "tcp://192.168.55.162:5055".

## Command Header

Any Command SHOULD follow this rule. First frame is used by the Router socket to identify 
destination. Second frame SHOULD be "SADA" followed by one byte which specify the version of the 
specification, for instance 0x01 for version 1.

Third frame defines the Command name and SHOULD be one of : "INTR","REQ","REP","PING" or "PONG".

## INTR Command

Introduce Command will be used to send the service catalog of the servers into channels. After 
the command header, every service comes with its frames. two frames for each service. We call 
these frames, "service frames". 

First frame of a service frames is the name of the service and second is its version.

below is a visual form of the INTR command:

===INTR Command Message===
* frame 0: identity of the server router socket
* frame 1: empty frame
* frame 2: "SADA1" #indicate the protocol name and version 
* frame 3: "INTR" #indicate the command type
* frame 4: "example-service" #name of the service
* frame 5: "1.2" #version of the example-service
* frame 6: "another-service"
* frame 7: "1"
...

## RINTR Command

Request for INTR command SHOULD send by channels after it lose its state probably by restarting. 
servers MUST response to RINTR with an INTR message.
below is a visual form of the RINTR command:

===RINTR Command Message===
* frame 0: identity of the channel router socket
* frame 1: empty frame
* frame 2:"SADA1" #indicate the protocol name and version
* frame 3: "RINTR" #indicate the command type

## REQ Command

Channels MAY send REQ commands to the servers to initiate a request. every REQ message after 
command header follows by one frame which is request id. which should be unique across the 
cluster. Channels RECOMMENDED to prepends their id in the request id generated. So after command 
header there SHOULD be a frame containing the request id and after that there SHOULD be two 
frames of service frames which request is related to. frames seven and eight dedicated to
the action, frame seven is a category of the action and frame eight is the action name.
frame nine is the actual request payload body. which MAY be formatted by the msgPack or 
protocolBuffer.
===REQ Command Message===

* frame 0 : identity of the server router socket
* frame 1: empty frame
* frame 2: "SADA1" #indicate the protocol name and version 
* frame 3: "REQ" #indicate the command type
* frame 4: "12345678" #request id
* frame 5: "customer-management" #name of the service
* frame 6: "1" #version of the service
* frame 7: "customer" #category of the action
* frame 8: "registerNewCustomer" #action name
* frame 9: [byte array] #request payload

## REP Command
Every REQ command SHOULD be responded by a REP message from server to channel. If server does not
reply within timeout boundary channel consider the server is busy and RECOMMENDED to lower its 
load on it. If no response receive within a specific time from a server and server does not 
answer the PING command server considered dead. After Command header fifth frame is the request 
id which this reply is correlated to. sixth frame is the status code and it's equal to HTTP 
status codes. seventh frame is reply payload formatted as byte array by msgPack or 
protocolBuffer.
 
===REP Command Message===
* frame 0 : identity of the server router socket
* frame 1: empty frame
* frame 2: "SADA1" #indicate the protocol name and version 
* frame 3: "REP" #indicate the command type
* frame 4: "12345678" #request id which indicate this response correlated to which request
* frame 5: 200 #status code
* frame 6: [byte array] #reply payload

## PING and POND command
Channels MAY send ping command to find disconnected servers. PING command just consist of a command 
header with the command type "PING".

===PING Command Message===
* frame 0 : identity of the server router socket
* frame 1: empty frame
* frame 2: "SADA1" #indicate the protocol name and version 
* frame 3: "PING" #indicate the command type

Servers SHOULD response any PING message with PONG message which is similar to PING but command 
type is "PONG".

===PONG Command Message===
* frame 0 : identity of the server router socket
* frame 1: empty frame
* frame 2: "SADA1" #indicate the protocol name and version 
* frame 3: "PONG" #indicate the command type

## Server Reliability

Channels MAY send ping commands at regular intervals when no other conversation happened in that 
interval. A channel SHOULD consider a server "disconnected" if no PONG arrives within some 
multiple of that interval (usually 2-3).