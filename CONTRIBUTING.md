# Contributing to Eclipse Marketplace Client

Thanks for your interest in this project.

## ⚖️ Legal and Eclipse Foundation terms

The project license is available at [LICENSE](LICENSE).

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

Before your contribution can be accepted by the project team, 
contributors must have an Eclipse Foundation account and 
must electronically sign the Eclipse Contributor Agreement (ECA).

* [http://www.eclipse.org/legal/eca/](https://www.eclipse.org/legal/eca/)

For more information, please see the Eclipse Committer Handbook:
[https://www.eclipse.org/projects/handbook/#resources-commit](https://www.eclipse.org/projects/handbook/#resources-commit).

## 💬 Get in touch with the community

Eclipse Marketplace Client uses issues:

* 🐞 View and report issues through GitHub Issues at https://github.com/eclipse-mpc/epp.mpc/issues.

Project committers must 📧 join the [mpc-dev@eclipse.org](https://accounts.eclipse.org/mailing-list/mpc-dev/) mailing list,
which must be used (according to the Eclipse Development Process) for formal project development decisions such as committer and project lead elections.

## 🆕 Trying latest builds

Latest builds, for testing, can be found at [`https://download.eclipse.org/mpc/updates/nightly/latest/`](https://download.eclipse.org/mpc/updates/nightly/latest/).

## 🧑‍💻 Developer resources

### Prerequisites

Java 25 and Maven 3.9.16 (only if you want to build from the command-line), or newer.
This is subject to upgrades over time

### 🧙 Setting up the Development Environment Automatically

You can set up a pre-configured IDE for contribution to Eclipse Marketplace Client using the following link:

[![Create Eclipse Development Environment for the Eclipse SDK](https://download.eclipse.org/oomph/www/setups/svg/Marketplace_Client.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-mpc/epp.mpc/master/setup/MPCConfiguration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag onto your running installer's title area")


### 🏗️ Build

Simply use `mvn clean verify` which will build and run the tests (`-DskipTests` to skip them).
The resulting p2 repository and specific IDE applications will be available for further manual testing in `repository/target`.

The automatically-configured development environment contains pre-configured `m2e` launch configurations for local Tycho builds.


### ➕ Submit changes

Eclipse Marketplace Client only accepts contributions via GitHub Pull Requests against [https://github.com/eclipse-mpc/epp.mpc](https://github.com/eclipse-mpc/epp.mpc) repository.
