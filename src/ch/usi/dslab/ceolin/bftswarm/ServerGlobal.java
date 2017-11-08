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

package ch.usi.dslab.ceolin.bftswarm;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceProxy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 */
public class ServerGlobal extends Server {

    private static Integer serverGroupId;
    private static ServiceProxy proxyToLocal1;
    private static ServiceProxy proxyToLocal2;
    private static ServiceProxy proxyToLocal3;

    public static class ProxyToLocal {
        public ProxyToLocal(int serverGlobalId, String localConfigPath1, String localConfigPath2, String localConfigPath3) {
            int clientId = 7000 + serverGlobalId;
            System.out.println("Connected to Local Replicas as ID: " + clientId);
            proxyToLocal1 = new ServiceProxy(clientId, localConfigPath1);
            proxyToLocal2 = new ServiceProxy(clientId, localConfigPath2);
            proxyToLocal3 = new ServiceProxy(clientId, localConfigPath3);
        }
    }

    public ServerGlobal(int serverGlobalId, int groupId, String configPath) {
        super(serverGlobalId, configPath);
        serverGroupId = groupId;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ServerGlobal <server id> <group id> <global config path> <local config path 1> <local config path 2> <local config path 3>");
            System.exit(0);
        }

        String globalConfigPath = args[2];
        String localConfigPath1 = args[3];
        String localConfigPath2 = args[4];
        String localConfigPath3 = args[5];

        // Start the client of the local group (ServiceProxy Class).
        // Expect a bad behavior if you start the ServerReplica fist.
        new ProxyToLocal(Integer.parseInt(args[0]), localConfigPath1, localConfigPath2, localConfigPath3);

        // Start the global server (ServiceReplica Class).
        new ServerGlobal(Integer.parseInt(args[0]), Integer.parseInt(args[1]), globalConfigPath);
    }

    @Override
    public byte[][] appExecuteBatch(byte[][] command, MessageContext[] mcs) {
        byte[][] replies = new byte[command.length][];
        for (int i = 0; i < command.length; i++) {
            replies[i] = executeGlobal(command[i], mcs[i]);
        }
        return replies;
    }

    //
    // executeSingle parses the request to identify if the operation type match to PUT or REMOVE
    // and them applies the operation into the TreeMap.
    // It's a good idea check all the info provided by the class MessageContext!
    //
    // msgCtx.getConsensusId()
    // msgCtx.getLeader()
    //
    private byte[] executeGlobal(byte[] command, MessageContext msgCtx) {
        ByteArrayInputStream in = new ByteArrayInputStream(command);
        DataInputStream dis = new DataInputStream(in);
        int reqType;
        try {
            reqType = dis.readInt();
            if (reqType == RequestType.PUT) {

                String key = dis.readUTF();
                String value = dis.readUTF();
                    JSONObject objValue = new JSONObject(value);
                    JSONArray arr = objValue.getJSONArray("targetGroup");
                    for (int i = 0; i < arr.length(); i++){

//                        System.out.println(arr.get(i));
//                        if(i == serverGroupId){
//                            System.out.println("Received key/value to forward: " + key + " / " + value);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            DataOutputStream dos = new DataOutputStream(out);
                            dos.writeInt(RequestType.PUT);
                            dos.writeUTF(key);
                            dos.writeUTF(value);
                            if(arr.getInt(i) == 1) {
                                byte[] reply1 = proxyToLocal1.invokeOrdered(out.toByteArray());
                            }
                            if(arr.getInt(i) == 2) {
                                byte[] reply2 = proxyToLocal2.invokeOrdered(out.toByteArray());
                            }
                            if(arr.getInt(i) == 3) {
                                byte[] reply3 = proxyToLocal3.invokeOrdered(out.toByteArray());
                            }
                            out.close();
//                        }
                    }
                return null;
            } else {
                System.out.println("Unknown request type: " + reqType);
                return null;
            }
        } catch (IOException e) {
            System.out.println("Exception reading data in the replica: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
