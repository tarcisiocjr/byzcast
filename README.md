# ByzCast

**ByzCast** is a proof of concept of a partial-genuine byzantine atomic multicast protocol using as base to implementation the library **[BFT-SMaRt][1]**. 
The idea behind this project is simple: we use multiple groups of instances of BFT-SMART and the data are partitioned between these groups. 
Messages addressed only to one group are routed locally and messages addressed to multiple groups are forwarded to a global group. 
After the global consensus the message is forwarded to the corresponding local(s) group(s).

[1]: https://github.com/bft-smart/library

## Installation

This project requires Java Runtime Environment 11 and Maven. 
To compile:
   
``make build-async`` to build the asynchronous version with models

``make build-sync`` to build the original version


## Usage

``make dev-async``

``make dev-sync``

You need TMUX to run the project with windows and panes index setted to 1. In your tmux.conf:

``set -g base-index 1``

``setw -g pane-base-index 1``


## License

ByzCast - A (hopeful) high-throughput BFT Atomic Multicast implementation
Copyright (C) 2017, University of Lugano

This file is part of ByzCast.

BFT-SWARM is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA