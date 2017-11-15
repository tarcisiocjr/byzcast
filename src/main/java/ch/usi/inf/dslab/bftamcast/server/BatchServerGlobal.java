/*
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
 */

package ch.usi.inf.dslab.bftamcast.server;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

import java.io.*;
import java.util.Arrays;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class ServerGlobal extends DefaultRecoverable {
    private int id;
    private int seqNumber;
    private ServiceProxy[] proxiesToLocal;
    private Thread[] invokeThreads;
    private byte[][] invokeReplies;

    public ServerGlobal(int serverGlobalId, String configPath, String[] localConfigPaths) {
        int clientId = 70000 + serverGlobalId;

        id = serverGlobalId;
        proxiesToLocal = new ServiceProxy[localConfigPaths.length];
        invokeThreads = new Thread[localConfigPaths.length];
        invokeReplies = new byte[localConfigPaths.length][];
        seqNumber = 1;
        try {
            Thread.sleep(localConfigPaths.length * 4000 + this.id * 1000);
        } catch (InterruptedException e) {
            System.err.println("Error starting server " + this.id);
            e.printStackTrace();
            System.exit(-1);
        }

        for (int i = 0; i < localConfigPaths.length; i++) {
            System.out.println("Connected to Local Group " + i + ", config '" + localConfigPaths[i] + "'  as ID: " + clientId);
            proxiesToLocal[i] = new ServiceProxy(clientId, localConfigPaths[i]);
        }
        new ServiceReplica(id, configPath, this, this, null, new DefaultReplier());
    }

    public static void main(String[] args) {
        CLIParser p = CLIParser.getGlobalServerParser(args);
        new ServerGlobal(p.getId(), p.getGlobalConfig(), p.getLocalConfigs());
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {
        byte[][] replies = new byte[command.length][];
        for (int i = 0; i < command.length; i++) {
            replies[i] = executeGlobal(command[i], mcs[i]);
        }
        return replies;
    }

    private byte[] executeGlobal(byte[] command, MessageContext msgCtx) {
        Request req = new Request(), reply = new Request();
        byte[] toSend;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        req.fromBytes(command);

        req.setSeqNumber(seqNumber++);
        toSend = req.toBytes();
        try {
            for (int dest : req.getDestination()) {
                invokeThreads[dest] = new Thread(() -> {
                    invokeReplies[dest] = proxiesToLocal[dest].invokeOrdered(toSend);
                });
                invokeThreads[dest].start();
            }

            for (int dest : req.getDestination()) {
                invokeThreads[dest].join();
                reply.fromBytes(invokeReplies[dest]);
                bos.write(reply.getValue());
            }
            bos.flush();
            bos.close();
            req.setValue(bos.toByteArray());
            return req.toBytes();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    // TreeMap to byte array
    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeInt(id);
            out.writeInt(seqNumber);
            out.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println("Exception when trying to take a + "
                    + "snapshot of the application state" + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }


    // Byte array to TreeMap
    @Override
    public void installSnapshot(byte[] state) {
        ByteArrayInputStream bis = new ByteArrayInputStream(state);
        try {
            ObjectInput in = new ObjectInputStream(bis);
            id = in.readInt();
            seqNumber = in.readInt();
            in.close();
            bis.close();
        } catch (IOException e) {
            System.out.print("Exception installing the application state: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("Implemented by PGAMcastReplier");
    }
}
