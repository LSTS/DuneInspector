# DuneInspector Wiki

## What it it?
DuneInspector allows generating UML diagrams from DUNE source code as well as IMC messages. It can be used to learn more about DUNE and better understand interactions between Tasks running in a DUNE system.

## Compiling
* To compile you need Oracle's Java Development Kit (JDK) 8 and Apache ANT.
* In the folder of the project, just run `ant inspect` and the *inspect* binary will be produced.
* Alternatively, you can run a [pre-compiled version](https://github.com/zepinto/DuneInspector/releases/latest).

## Running
* Provided you Java 8 and GraphViz installed, you can run DuneInspector by executing the *inspect* binary simply by running: `./inspect` which will give you a list of accepted arguments.

## Running Examples
* Obtain the UML (message fields) for a given IMC message: `./inspect -msg TrexToken`:

![Message UML](https://raw.githubusercontent.com/zepinto/DuneInspector/master/examples/TrexToken.png)

* Obtain the class diagram of a given DUNE task: `./inspect -task Transports.GSM`:

![Task class diagram](https://raw.githubusercontent.com/zepinto/DuneInspector/master/examples/TransportsGSM2.png)

* Obtain the communications diagram of a given DUNE task: `./inspect -comms Transports.GSM`

![Task comms diagram](https://raw.githubusercontent.com/zepinto/DuneInspector/master/examples/TransportsGSM.png)


