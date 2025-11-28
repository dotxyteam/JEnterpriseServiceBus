JEnterpriseServiceBus
=====================

![alt JESB icon](https://github.com/dotxyteam/JEnterpriseServiceBus/blob/main/j-enterprise-service-bus/misc/GUI-screenshots/components/mix.jpg?raw=true)

# Overview

It is a Java-based open source visual development platform allowing to automate business processes and connect heterogeneous applicative systems. 

It has a graphical user interface for process modeling and debugging. 
![alt GUI](https://github.com/dotxyteam/JEnterpriseServiceBus/blob/main/j-enterprise-service-bus/misc/GUI-screenshots/gui.png?raw=true)

The dynamism/intelligence of processes is expressed through (essentially) generated Java code snippets, which eliminates the need to learn any specific syntax.

Extensibility is almost a key feature allowing any user to model and mostly generate plugins complementing core features as needed.


# Use Cases

- Integrating heterogeneous applications (REST/SOAP, XML, database, ...)
- Developing ETL (extract, transform, and load) / ESB (Enterprise Service Bus) jobs
- Orchestrating business processes
- …

# Advantages

- Visual process development (low code) and debugging
- Java-based expression editor (no specific language to learn)
- Simplified extensibility (plugins generator available)
- Easy reusability/integration (Maven dependency available)

# Compatibility

Tested on Windows & Linux.

# Licensing

It is distributed under this
[license](https://github.com/dotxyteam/JEnterpriseServiceBus/blob/master/j-enterprise-service-bus/LICENSE).

# Download

*  [Get the source code and the binaries↓](https://github.com/dotxyteam/JEnterpriseServiceBus/releases)

# Getting started

*   Get the archives from the download area (see above)
    *   Choose the one corresponding to your OS
    *   Extract it
*   Run the executable (with "--help" command-line argument to see available options).
    *   IMPORTANT:
        *    On Windows you must run the CMD executable (**j-enterprise-service-bus-cmd.exe**) if you intend to use the command-line options.

# Concepts

- JESB allows to develop one **solution** at a time.
- Each **solution** is composed of **plans** and **resources**.
- Each **plan** has its own activation mode (**activator**).
- **Resources** are shared configurations referenced by **plans** or other **resources**.

# Java API

JESB processes can be called from any Java code. 
The required Maven dependency is as follows:

    <dependency>
    <groupId>com.github.dotxyteam</groupId>
    <artifactId>j-enterprise-service-bus</artifactId>
    <version>LATEST</version>
    </dependency>

The following class demonstrates the use of this API: 
https://github.com/dotxyteam/JEnterpriseServiceBus/blob/main/j-enterprise-service-bus/unpackaged-src/com/otk/jesb/JesbAPIExample.java

# Support

The support page is hosted [here on GitHub](https://github.com/dotxyteam/JEnterpriseServiceBus/issues). You can also contact us by email: [dotxyteam@yahoo.fr](mailto:dotxyteam@yahoo.fr).
