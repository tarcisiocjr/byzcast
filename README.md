# BFT-SWARM

BFT-SWARM is a (hopeful) high-throughput [BFT-SMART][1] implementation. The idea behind this project is simple: we use groups of instances of BFT-SMART and the data are partitioned between these groups. Messages that target only one group are routed locally and messages that are targeted for multiple groups are forwarded to a global group (containing all replicas). After the consensus, the message is forwarded to the corresponding local group.

[1]: https://github.com/bft-smart/library

## Installation

This project requires Java Runtime Environment 1.7 or later to running. For development, tests and deploy I recommend that you have [Docker][2] version 17 or later installed.

[2]: https://www.docker.com

## Usage

You should noted that we have a Makefile, so it's straight forward. Normally after change config files, you probably `make build` or `make up console` it's what you are looking for.

## Contributing

1. Fork it
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## History

TODO: Write history

## Credits

TODO: Write credits

## License

BFT-SWARM - A (hopeful) high-throughput BFT-SMART implementation
Copyright (C) 2017, University of Lugano

This file is part of BFT-SWARM.

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



serverGroupId
serverLocalId
clientLocalId


- Identificador da Réplica Localmente:

- Identificador do Grupo Localmente:


- Identificador da Réplica Globalmente:

- Identificador do Encaminhador para Grupo Local:
