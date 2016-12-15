# DuneInspector Wiki

## What it it?
DuneInspector allows generating UML diagrams from DUNE source code as well as IMC messages. It can be used to learn more about DUNE and better understand interactions between Tasks running in a DUNE system.

## Compiling
* To compile you need Oracle's Java Development Kit (JDK) 8 and Apache ANT.
* In the folder of the project, just run `ant inspect` and the *inspect* binary will be produced.

## Running
* Provided you Java 8 and GraphViz installed, you can run DuneInspector by executing the *inspect* binary simply by running: `./inspect` which will give you a list of accepted arguments.

## Running Examples
* Obtain the UML (message fields) for a given IMC message: `./inspect -msg TrexToken`

[[https://github.com/zepinto/DuneInspector/blob/master/examples/TrexToken.png|alt=octocat]]

* Obtain the class diagram of a given DUNE task: `./inspect -task Transports.GSM`

[[https://github.com/zepinto/DuneInspector/blob/master/examples/TransportsGSM2.png|alt=octocat]]

* Obtain the communications diagram of a given DUNE task: `./inspect -comms Transports.GSM`

[[https://github.com/zepinto/DuneInspector/blob/master/examples/TransportsGSM.png|alt=octocat]]

