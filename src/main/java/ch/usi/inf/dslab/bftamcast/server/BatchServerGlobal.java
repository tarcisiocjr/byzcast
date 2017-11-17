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
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

import java.io.*;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class BatchServerGlobal extends DefaultRecoverable {
    private final int[] allDest;
    private int id;
    private int seqNumber, innerSeqNumber;
    private ServiceProxy[] proxiesToLocal;
    private Thread[] invokeThreads;
    private byte[][] invokeReplies;
    private boolean nonGenuine;

    private BatchServerGlobal(int serverGlobalId, String configPath, String[] localConfigPaths, boolean ng) {
        int clientId = 80000 + serverGlobalId;
        id = serverGlobalId;
        proxiesToLocal = new ServiceProxy[localConfigPaths.length];
        invokeThreads = new Thread[localConfigPaths.length];
        invokeReplies = new byte[localConfigPaths.length][];
        allDest = new int[localConfigPaths.length];
        innerSeqNumber = seqNumber = 1;
        nonGenuine = ng;
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
            allDest[i] = i;
        }
        new ServiceReplica(id, configPath, this, this, null, new DefaultReplier());
    }

    public static void main(String[] args) {
        CLIParser p = CLIParser.getGlobalServerParser(args);
        new BatchServerGlobal(p.getId(), p.getGlobalConfig(), p.getLocalConfigs(), p.isNonGenuine());
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {
        //System.out.println("batch size = " + command.length);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[][] replies = new byte[command.length][];
        Request mainReq = new Request(), auxReq = new Request();
        Request[] reqs = new Request[command.length];

        try {
            mainReq.setType(RequestType.BATCH);
            mainReq.setDestination(allDest);
            mainReq.setSeqNumber(seqNumber++);
            //System.out.println("batch size = " + command.length + ", seq. number = " + mainReq.getSeqNumber());

            for (int i = 0; i < reqs.length; i++) {
                reqs[i] = new Request();
                reqs[i].fromBytes(command[i]);
                reqs[i].setSeqNumber(innerSeqNumber++);
            }

            mainReq.setValue(Request.ArrayToBytes(reqs));
            for (int dest : allDest) {
                invokeThreads[dest] = new Thread(() -> invokeReplies[dest] = proxiesToLocal[dest].invokeOrdered(mainReq.toBytes()));
                invokeThreads[dest].start();
            }

            //reset values
            for (int i = 0; i < command.length; i++)
                reqs[i].setValue(null);


            for (int dest : allDest) {
                invokeThreads[dest].join();
                auxReq.fromBytes(invokeReplies[dest]);
                Request[] temp = Request.ArrayfromBytes(auxReq.getValue());
                //System.out.println("reply from group " + dest + ": req = " + auxReq + ", reply size = " + temp.length);
                for (int i = 0; i < temp.length; i++) {
                    bos.reset();
                    if (temp[i].getType() != RequestType.NOP) { // message was addressed to group dest
                        if (reqs[i].getValue() == null) { //set value initially to false
                            reqs[i].setValue(temp[i].getValue());
                        } else {
                            bos.write(reqs[i].getValue());
                            bos.write(temp[i].getValue());
                            bos.close();
                            reqs[i].setValue(bos.toByteArray());
                        }
                    }
                }
                auxReq.setValue(null);
            }

            for (int i = 0; i < command.length; i++)
                replies[i] = reqs[i].toBytes();

        } catch (InterruptedException | IOException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.exit(20);
        }

        return replies;

    }

    // TreeMap to byte array
    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeInt(id);
            out.writeInt(seqNumber);
            out.writeInt(innerSeqNumber);
            out.writeBoolean(nonGenuine);
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
            innerSeqNumber = in.readInt();
            nonGenuine = in.readBoolean();
            in.close();
            bis.close();
        } catch (IOException e) {
            System.out.print("Exception installing the application state: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        throw new UnsupportedOperationException("Global server only accepts ordered messages");
    }
}
