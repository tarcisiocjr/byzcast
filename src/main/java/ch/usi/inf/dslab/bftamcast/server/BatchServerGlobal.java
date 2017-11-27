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
import bftsmart.tom.server.Replier;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import bftsmart.tom.server.defaultservices.DefaultReplier;
import ch.usi.inf.dslab.bftamcast.kvs.Request;
import ch.usi.inf.dslab.bftamcast.kvs.RequestType;
import ch.usi.inf.dslab.bftamcast.util.CLIParser;

import java.io.*;
import java.util.Arrays;

/**
 * @author Paulo Coelho - paulo.coelho@usi.ch
 */
public class BatchServerGlobal extends DefaultRecoverable {
    private static final int snDelta = 20;
    private final int[] allDest;
    private int id, groupId;
    private int seqNumber, innerSeqNumber;
    private ServiceProxy[] proxies;
    private Thread[] invokeThreads;
    private byte[][] invokeReplies;
    private Replier rep;

    private BatchServerGlobal(int serverGlobalId, int groupId, String[] configPaths, String[] localConfigPaths) {
        int clientId = 80000 + serverGlobalId;
        int sleepTime = 0;
        String[] configs = null; // path for groups to connect
        String myConfig = null;  // my config path

        id = serverGlobalId;
        this.groupId = groupId;
        innerSeqNumber = seqNumber = groupId;

        if (configPaths.length == 1) {
            System.out.println("******UNIQUE GLOBAL GROUP******");
            proxies = new ServiceProxy[localConfigPaths.length];
            sleepTime = proxies.length * 5000 + this.id * 1500;
            configs = localConfigPaths;
            myConfig = configPaths[0];
            rep = new DefaultReplier();
        } else if (configPaths.length == (localConfigPaths.length / 2 + 1)) {
            System.out.println("******2-LEVEL GLOBAL GROUPS******");
            if (groupId == 0) {
                System.out.println("-.-.-.-TOP LEVEL-.-.-.-");
                proxies = new ServiceProxy[configPaths.length - 1];
                sleepTime = (localConfigPaths.length + 1) * 5000 + this.id * 1500;
                configs = Arrays.copyOfRange(configPaths, 1, configPaths.length);
                myConfig = configPaths[groupId];
                rep = new DefaultReplier();

            } else {
                System.out.println("-.-.-.-INTERMEDIATE LEVEL-.-.-.-");
                clientId += groupId * 10000;
                proxies = new ServiceProxy[2];
                sleepTime = localConfigPaths.length * 5000 + this.id * 1500;
                configs = new String[]{
                        localConfigPaths[2 * groupId - 2],
                        localConfigPaths[2 * groupId - 1]
                };
                myConfig = configPaths[groupId];
                rep = new GlobalBatchReplier(this);
            }

        } else {
            System.err.println("INCOMPATIBLE NUMBER OF GLOBAL GROUPS.");
            System.exit(1);
        }

        invokeThreads = new Thread[proxies.length];
        invokeReplies = new byte[proxies.length][];
        allDest = new int[proxies.length];
        try {
            Thread.sleep(sleepTime);
            for (int i = 0; i < configs.length; i++) {
                System.out.println("Connected to Group with config '" + configs[i] + "'  as ID: " + clientId);
                proxies[i] = new ServiceProxy(clientId, configs[i]);
                allDest[i] = i;
            }
            new ServiceReplica(id, myConfig, this, this, null, rep);
        } catch (InterruptedException e) {
            System.err.println("Error starting server " + this.id);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        CLIParser p = CLIParser.getGlobalServerParser(args);
        new BatchServerGlobal(p.getId(), p.getGroup(), p.getGlobalConfig(), p.getLocalConfigs());
    }

    int getGroupId() {
        return groupId;
    }

    int getNextSeqNumber() {
        seqNumber += snDelta;
        return seqNumber;
    }

    int getNextInnerSeqNumber() {
        innerSeqNumber += snDelta;
        return innerSeqNumber;
    }


    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {
        if (groupId == 0) {
            //System.out.println("batch size = " + command.length);
            Request[] reqs = new Request[command.length];
            byte[][] replies = new byte[reqs.length][];

            for (int i = 0; i < reqs.length; i++) {
                reqs[i] = new Request();
                reqs[i].fromBytes(command[i]);
                reqs[i].setSeqNumber(getNextInnerSeqNumber());
                //System.out.println("adding to batch: " + reqs[i]);
            }
            reqs = send(reqs);
            for (int i = 0; i < reqs.length; i++)
                replies[i] = reqs[i].toBytes();
            return replies;
        } else {
            ((GlobalBatchReplier) rep).setBatchSize(command.length);
            return command;
        }
    }

    public Request[] send(Request[] reqs) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Request mainReq = new Request(), auxReq = new Request();

        try {
            mainReq.setType(RequestType.BATCH);
            mainReq.setDestination(allDest);
            mainReq.setSeqNumber(getNextSeqNumber());
            mainReq.setValue(Request.ArrayToBytes(reqs));
            //System.out.println("batch size = " + command.length + ", seq. number = " + mainReq.getSeqNumber());

            for (int dest : allDest) {
                invokeThreads[dest] = new Thread(() -> invokeReplies[dest] = proxies[dest].invokeOrdered(mainReq.toBytes()));
                invokeThreads[dest].start();
            }

            for (int i = 0; i < reqs.length; i++)
                reqs[i].setValue(null);

            for (int dest : allDest) {
                invokeThreads[dest].join();
                auxReq.fromBytes(invokeReplies[dest]);
                Request[] temp = Request.ArrayfromBytes(auxReq.getValue());
                //System.out.println("reply from " + dest + ": req = " + auxReq + ", reply size = " + temp.length);
                for (int i = 0; i < temp.length; i++) {
                    bos.reset();
                    //System.out.println("\t" + temp[i]);
                    if (temp[i].getValue() != null) { // message was addressed to group dest
                        if (reqs[i].getValue() == null) {
                            reqs[i].setValue(temp[i].getValue());
                        } else {
                            bos.write(reqs[i].getValue());
                            bos.write(temp[i].getValue());
                            bos.close();
                            reqs[i].setValue(bos.toByteArray());
                        }
                    }
                    //else {
                    //System.out.println("NULL VALUE!!!!");
                    //}
                }
                auxReq.setValue(null);
            }
        } catch (InterruptedException | IOException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.exit(20);
        }

        return reqs;
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
