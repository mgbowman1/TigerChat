TigerChat :tiger:
=================
Chat with your fellow tigers.

This document details the solutions for network communications used by TigerChat.  
TigerChat utilizes User Datagram Protocol to send/receive datagrams, Reliable Data Protocol to ensure reliable transmission of datagrams and Tiger Transfer Protocol to establish communication between clients and the server.

Table of Contents: 
* [Requirements](#requirements)
* [Reliable Data Protocol](#reliable-data-protocol)
* [Tiger Transfer Protocol](#tiger-transfer-protocol)
* [Tiger Object Notation](#tiger-object-notation)

# Attributions:

* Background: <a href="https://www.vecteezy.com/">Vector Graphics by www.vecteezy.com</a> 

# Requirements:

* Python 3
* PyQt5

# Reliable Data Protocol:

RDP is a transport protocol that extends UDP to create a reliable data transmission 
protocol using UDP. A RDP socket will create a UDP socket to send and receive 
datagrams and the data portion of these datagrams will contain RDP datagrams.

## Structure

Total size of headers: 16 bytes

Header | Bytes
-------|-------
Sequence | 4 bytes
Acknoweledgment | 4 bytes
Head | 4 bytes
Tail | 4 bytes

## Flow Control

In RDP the maximum number of packets that will be sent per round of transmission is 
210 datagrams. Each RDP socket will track how many packets it has sent each round and will use ACK's to determine how many datagrams have been processed by the receiver
and adjust it's transmission rate accordingly.

## Timeout Interval

The timeout interval will be managed by an **exponential weighted moving average (EWMA)** and an estimate of the round trip time (RTT) between each participant. First an estimate of  the round trip time will be calculated by the following, where the Sample<sub>RTT</sub> is the RTT of one of the unacknowledged datagrams:

<pre><code>Estimate<sub>RTT</sub> = (1 - a) * Estimate<sub>RTT</sub> + a * Sample<sub>RTT</sub></code></pre>  

There will be another **EWMA** for the fluctuation between each Sample<sub>RTT</sub> called Dev<sub>RTT</sub>, which is the following:

<pre><code>Dev<sub>RTT</sub> = (1 - b) * Dev<sub>RTT</sub> + b * | Sample<sub>RTT</sub> - Estimate<sub>RTT</sub> |</code></pre>

The Timeout Interval is then computed by:

<pre><code>Timeout Interval = Estimate<sub>RTT</sub> + 4 * Dev<sub>RTT</sub></code></pre>

### Timer Thread

RDP implements a timer thread that keeps track of timeout events. It begins by tracking the first datagram in the send window after which it will wait for the acknowledgment for that datagram. Once the acknowledgement is received, it finds the next unacknowledged datagram and, based on its timestamp, will signal that a timeout has occured. It will continue until every datagram in the send window is acknowledged. If a timeout has occured for any element it will signal a timeout and continue tracking the timeout of the next unacknowledged datagram; wrapping around the end of the list.

## Congestion Avoidance

RDP implements a congestion avoidance scheme similar to that of TCP Reno, using a two-phase system: slow start and congestion avoidance. RDP limits its  transmission of datagrams to equal the Congestion Window, which begins at 1. After each round of transmission, RPD waits until it receives acknowledgment of sent datagrams. After each successful round (where every datagram sent has been acknowledged), RPD will double its congestion window, this being known as the slow start phase. It will continue this process until it exceeds ssthresh (slow start threshold), after which, it will only increase the congestion window by 1 each round, this being known as the congestion avoidance phase. The window will continue growing until a timeout occurs. When a timeout occurs the congestion window will be set to 1. The ssthresh will be set to half of the congestion window from before the timeout occurred and the phase will be set to slow start.

## Implementation

### Sending/Receiving a Datagram

System A builds a datagram and sends it over the network to System B as well as tracking it as a pending datagram. If System B receives the datagram, it will build and send an acknowledgement datagram back to System A. After this, System B will process the datagram and forward it to the next layer. If System A receives the acknowledgement, it will remove the datagram from the list of pending datagrams. In the case that the original datagram is lost, after a timeout occurs, System A will immediately resend the datagram before resuming regular operation. If the acknowledgement is lost then, after receiving three acknowledgements for later datagrams or when a timeout occurs, System A will resend the datagram. If three timeouts occur for either system, they will assume the connection is lost.  
Each transmission round, the number of datagrams to be sent will be determined by the send window. Each of these datagrams will be sent and placed into a list of pending datagrams. System A will then process received datagrams, listening for acknowledgements to each pending datagram. Until every datagram in the pending list has been acknowledged, System A will not enter another transmission round.

### Fragmented Datagrams

When processing a fragmented datagram stream, the head # of the datagram will be set to the sequence number of the first datagram in the stream and the tail # will be set to the sequence # of the last datagram in the stream. The socket, at the receiving end, will create a single non-fragmented packet built from each of the individual fragments. After the entire packet is completed, it will be forwarded to the next layer. After each transmission round, a progress packet will be built and forwarded to the next layer to give information on the current status and number of sent/received fragments.

# Tiger Transfer Protocol:

The application layer protocol used by TigerChat. It enables both message and file
transmission. Tiger Transfer Protocol is built to manage network operations for
TigerChat. The MTU is 512 bytes with 487 bytes of data.

## Structure

Total size of headers: 1 byte
The flag will dictate the structure of the data

Header | Bytes
-------|------
Flag | 1 byte

The Flags are as follows:

ID | Name | Content
----|----|----
MSG | Message | Text data
FIL | File | Sent/Received file data in entirety
INF | File Information | Filename, size
RQM | Request Message | Conversation ID and Message Block number / TON object
FRG | Fragment Progress | Report on current progress of sending/receiving fragmented packet
CON | Connect | Username and password / port
CLS | Close | No data
CCV | Create Conversation | Members of conversations and ID

The Data structures are as follows:

* Message: `Sender ID | Conversation ID | Timestamp | Message`

* File: `File Data`

* File Information: `Sender ID | Conversation ID | Timestamp | Size | Filename`

* Request Message from Client: `Conversation ID | Message Block Number`

* Request Message from Server: `TON object`

* Fragment Progress: `File ID | Received Percentage`

* Connect from Client: `Username | Password`

* Connect from Server: `Port`

* Close: `(no data)`

* Create Conversation from Client: `Username ID | Username ID ...`

* Create Conversation from Server: `Conversation ID`

## Establish a Connection

The client will send a CON packet to the server. The server will respond with another CON packet with the port that belongs to their session.

# Message Block Number

A number identifying a set of 50 messages within a particular conversation. This should amount to a maximum of 400kB.

# Tiger Object Notation

A human readable file format for transmitting fixed-dynamic structured data for sending lists of messages in TigerChat.

## Structure

A TON object is formatted as follows:

	[{Sender ID,Conversation ID,Timestamp," Escaped Message "},{ ... }]

* The Sender ID is a number that identifies the sender of the message.
* The Conversation ID is a number that identifies the conversation the message is in.
* The timestamp is a string representing the date and time the message was sent.
* The escaped message is the text message which was sent, such that each double quote and backslash has another backslash preceeding it and the whole message is surrounded by double quotes.
