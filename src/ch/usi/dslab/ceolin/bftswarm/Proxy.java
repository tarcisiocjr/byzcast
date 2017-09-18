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

/**
 * @author Tarcisio Ceolin - tarcisio.ceolin.junior@usi.ch
 */

import bftsmart.tom.ServiceProxy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Proxy implements Map<String, String> {

    private static String configLocal = "config-local";
    private static String configGlobal = "config-global";
    private static Integer proxyGroupId;

    private ServiceProxy clientProxyLocal = null;
    private ServiceProxy clientProxyGlobal = null;

    public Proxy(int clientId, int groupId) {
        clientProxyLocal = new ServiceProxy(clientId, configLocal);
        clientProxyGlobal = new ServiceProxy(clientId, configGlobal);
        proxyGroupId = groupId;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<String> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String put(String key, String value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        JSONObject objValue = new JSONObject(value);

        try {

            // Check if the target group of the message contains only one
            // value and if the the value is equals to the group id of proxy.
            // If yes, send the message only to the local group.
            // Anything else, it sends to the global group.
            JSONArray arr = objValue.getJSONArray("targetGroup");
            if (arr.length() == 1) {
                int targetGroupId = arr.getInt(0);
                if (targetGroupId == proxyGroupId) {
                    System.out.println("Local group: " + targetGroupId);
                    System.out.println("Local group message: " + value);
                    dos.writeInt(RequestType.PUT);
                    dos.writeUTF(key);
                    dos.writeUTF(value);
                    byte[] replyLocal = clientProxyLocal.invokeOrdered(out.toByteArray());
                    if (replyLocal != null) {
                        String previousValue = new String(replyLocal);
                        return previousValue;
                    }
                    return null;
                }
            } else {
                    System.out.println("Global group: " + arr);
                    System.out.println("Global group message: " + value);
                    dos.writeInt(RequestType.PUT);
                    dos.writeUTF(key);
                    dos.writeUTF(value);
                    byte[] replyGlobal = clientProxyGlobal.invokeOrdered(out.toByteArray());
                    if (replyGlobal != null) {
                        String previousValue = new String(replyGlobal);
                        return previousValue;
                    }
                    return null;
            }
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception putting value into hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String get(Object key) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeInt(RequestType.GET);
            dos.writeUTF(String.valueOf(key));
            byte[] replyLocal = clientProxyLocal.invokeUnordered(out.toByteArray());
            String value = new String(replyLocal);
            return value;
        } catch (IOException ioe) {
            System.out.println("Exception getting value from the hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public String remove(Object key) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        try {
            dos.writeInt(RequestType.REMOVE);
            dos.writeUTF(String.valueOf(key));
            byte[] replyLocal = clientProxyLocal.invokeOrdered(out.toByteArray());
            if (replyLocal != null) {
                String removedValue = new String(replyLocal);
                return removedValue;
            }
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception removing value from the hashmap: " + ioe.getMessage());
            return null;
        }
    }

    @Override
    public int size() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeInt(RequestType.SIZE);
            byte[] reply = clientProxyLocal.invokeUnordered(out.toByteArray());
            ByteArrayInputStream in = new ByteArrayInputStream(reply);
            DataInputStream dis = new DataInputStream(in);
            int size = dis.readInt();
            return size;
        } catch (IOException ioe) {
            System.out.println("Exception getting the size the hashmap: " + ioe.getMessage());
            return -1;
        }
    }
}
