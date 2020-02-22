package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.PriorityQueue;

import android.content.ContentResolver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

public class SimpleDhtProvider extends ContentProvider {

    static final String[] remotePorts = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    static int count = 0;
    List<Node> nodeList = new ArrayList<Node>();
    Node myNode = new Node();

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

    public void deleteValue(String selection){
        Context context = getContext();
        File key = new File(context.getFilesDir(),selection);
        if(key.exists()){
            key.delete();
        }
    }

    public void deleteAll(){
        Context context = getContext();
        File dirFiles = context.getFilesDir();
        for(File f : dirFiles.listFiles()){
            if(f.exists()){
                f.delete();
            }
        }
    }

    public void deleteSuccessor(String msgToSend){
        Socket socket = null;
        Log.d(TAG, "deleteSuccessor: " + msgToSend);
        try {
            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(myNode.getSuccessor()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(msgToSend);
            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            String recStr = in.readUTF();
            Log.d(TAG, "doInBackground: string received from server " + recStr);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    public void deleteFunc(String selection){
        String keyHash = null;
        String mySuccEmul = String.valueOf(Integer.parseInt(myNode.getSuccessor())/2);
        String mySuccHash = null;
        try {
            keyHash = genHash(selection);
            mySuccHash = genHash(mySuccEmul);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "deleteFunc: fileName " + selection + " keyhash "+ keyHash);
        String nodeId = myNode.getNode_id();
        String msg = null;

        if((selection.equals("*") && myNode.getPort_num().equals(myNode.getSuccessor())) || selection.equals("@")){
            Log.d(TAG, "delete: entered if block");
            deleteAll();
            return;
        }else if(selection.equals("*")){
            Log.d(TAG, "delete: entered else if block");
            deleteAll();
            deleteSuccessor("DeleteAll;" + myNode.getPort_num() + ";" +selection);
            return;
        }else {
            Log.d(TAG, "delete: entered else block");
            if (myNode.getPort_num().compareTo(myNode.getSuccessor()) == 0) {
                deleteValue(selection);
                return;
            } else if (keyHash.compareTo(myNode.getNode_id()) > 0 && keyHash.compareTo(mySuccHash) < 0) {
                Log.d(TAG, "deleteFunc: delete block 1 " + keyHash);
                msg = "Delete;" + selection;
            } else if (keyHash.compareTo(myNode.getNode_id()) > 0 && keyHash.compareTo(mySuccHash) > 0 && myNode.getNode_id().compareTo(mySuccHash) > 0) {
                Log.d(TAG, "deleteFunc: delete block 2 " + keyHash);
                msg = "Delete;" + selection;
            } else if (keyHash.compareTo(myNode.getNode_id()) < 0 && keyHash.compareTo(mySuccHash) < 0 && myNode.getNode_id().compareTo(mySuccHash) > 0) {
                Log.d(TAG, "deleteFunc: delete block 3 " + keyHash);
                msg = "Delete;" + selection;
            } else {
                Log.d(TAG, "deleteFunc: deleteNext block " + keyHash);
                msg = "DeleteNext;" + selection;
            }
        }
        Log.d(TAG, "deleteFunc: " + msg);
        Log.d(TAG, "deleteFunc: sending msg to " + myNode.getSuccessor());
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myNode.getSuccessor());
        return;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "delete: ");
        deleteFunc(selection);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub

        return null;
    }

    public void insertValue(Context context, String fileName, String valueToStore){
        try{
            OutputStreamWriter osw = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE));
            osw.write(valueToStore);
            osw.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        Log.v("insert fileName", fileName);
        Log.v("insert value", valueToStore);
    }

    public void insertFunc(String fileName, String valueToStore){
        Context context = getContext();

        String keyHash = null;
        String mySuccEmul = String.valueOf(Integer.parseInt(myNode.getSuccessor())/2);
        String mySuccHash = null;
        try {
            keyHash = genHash(fileName);
            mySuccHash = genHash(mySuccEmul);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "insertFunc: fileName " + fileName + " keyhash "+ keyHash);
        String nodeId = myNode.getNode_id();
        String msg = "";

        if (myNode.getPort_num().compareTo(myNode.getSuccessor()) == 0) {
            insertValue(context, fileName, valueToStore);
            return;
        }else if(keyHash.compareTo(myNode.getNode_id()) > 0 && keyHash.compareTo(mySuccHash) < 0){

            Log.d(TAG, "insertFunc: insert block 1 " + keyHash);
            msg = "Insert;"+fileName+";"+valueToStore;
        }else if(keyHash.compareTo(myNode.getNode_id()) > 0 && keyHash.compareTo(mySuccHash) > 0 && myNode.getNode_id().compareTo(mySuccHash) > 0){

            Log.d(TAG, "insertFunc: insert block 2 " + keyHash);
            msg = "Insert;"+fileName+";"+valueToStore;
        }else if(keyHash.compareTo(myNode.getNode_id()) < 0 && keyHash.compareTo(mySuccHash) < 0 && myNode.getNode_id().compareTo(mySuccHash) > 0){

            Log.d(TAG, "insertFunc: insert block 3 " + keyHash);
            msg = "Insert;"+fileName+";"+valueToStore;
        }else{
            Log.d(TAG, "insertFunc: insertNext block " + keyHash);
            msg = "InsertNext;"+fileName+";"+valueToStore;
        }
        Log.d(TAG, "insertFunc: " + msg);
        Log.d(TAG, "insertFunc: sending msg to " + myNode.getSuccessor());
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myNode.getSuccessor());
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        String fileName = values.getAsString("key");
        String valueToStore = values.getAsString("value");
        insertFunc(fileName, valueToStore);
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return false;
        }

        myNode.setEmulator_num(portStr);
        try {
            myNode.setNode_id(genHash(portStr));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        myNode.setPort_num(myPort);
        myNode.setPredecessor(myPort);
        myNode.setSuccessor(myPort);

        Log.d(TAG, "doInBackground: " + "MyNode : " + myNode.getPort_num());
        Log.d(TAG, "doInBackground: " + "MyNode P: " + myNode.getPredecessor());
        Log.d(TAG, "doInBackground: " + "MyNode S: " + myNode.getSuccessor());

        if(myPort.equals("11108")){
            nodeList.add(myNode);
        }else{
            String msg = "Join;"+portStr;
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
        }
        return false;
    }

    public String singleKey(String selection){
        String msg = "";
        Context context = getContext();

//        MatrixCursor cursor = new MatrixCursor(new String[] {"key", "value"});
        try {
            InputStreamReader isr = new InputStreamReader(context.openFileInput(selection));
            BufferedReader br = new BufferedReader(isr);
            msg = br.readLine();
//            String record [] = {selection, msg};
//            cursor.addRow(record);
            Log.v("query", selection);
            Log.v("queryAns", msg);
//            return cursor;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return selection+":"+msg;
    }

    public String allKeys(){
        String finalStr = "";
        String msg = null;
        Context context = getContext();

        try {
            File dirFiles = context.getFilesDir();
            for (String strFile : dirFiles.list())
            {
                InputStreamReader isr = new InputStreamReader(context.openFileInput(strFile));
                BufferedReader br = new BufferedReader(isr);
                msg = br.readLine();
                finalStr += strFile+":"+msg+";";
//                String record [] = {strFile, msg};
//                cursor.addRow(record);
                Log.v("query", strFile);
                Log.v("queryAns", msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return finalStr;
    }

    public Cursor getCursor(String curStr){
        MatrixCursor cursor = new MatrixCursor(new String[] {"key", "value"});
        if(curStr.equals("")){
            return cursor;
        }
        String[] record = curStr.split(";");
        for(String r : record){
            String[] v = r.split(":");
            cursor.addRow(new String[]{v[0], v[1]});
        }
        return cursor;
    }

    public String querySuccessor(String msg){
        Socket socket = null;
        Log.d(TAG, "querySuccessor: " + msg);
        try {
            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(myNode.getSuccessor()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(msg);
            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            String recStr = in.readUTF();
            Log.d(TAG, "doInBackground: string received from server " + recStr);
            socket.close();
            return recStr;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public String queryFunc(String selection){
        String keyHash = null;
        String mySuccEmul = String.valueOf(Integer.parseInt(myNode.getSuccessor())/2);
        String mySuccHash = null;
        String msg = "";
        try {
            keyHash = genHash(selection);
            mySuccHash = genHash(mySuccEmul);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "queryFunc: " + keyHash + " " + myNode.getNode_id()+ " " + mySuccHash);
        Log.d(TAG, "queryFunc: " + myNode.getPredecessor() + " " + myNode.getSuccessor());
        if((selection.equals("*") && myNode.getPort_num().equals(myNode.getSuccessor())) || selection.equals("@")){
            Log.d(TAG, "query: entered if block");
            return allKeys();
        }else if(selection.equals("*")){
            Log.d(TAG, "query: entered else if block");
            //call successor
            msg = querySuccessor("QueryAll;" + myNode.getPort_num() + ";" +selection);
            msg += allKeys();
            return msg;
        }else{
            Log.d(TAG, "query: entered else block");
            if (myNode.getPort_num().equals(myNode.getSuccessor())) {
                Log.d(TAG, "queryFunc: single key selection");
                return singleKey(selection);
            } else if(keyHash.compareTo(myNode.getNode_id()) > 0 && keyHash.compareTo(mySuccHash) < 0){
                Log.d(TAG, "queryFunc: successor node");
                msg = "Query;" + selection;
                return querySuccessor(msg);
            } else if(keyHash.compareTo(myNode.getNode_id()) > 0 && keyHash.compareTo(mySuccHash) > 0 && myNode.getNode_id().compareTo(mySuccHash) > 0){
                Log.d(TAG, "queryFunc: successor node");
                msg = "Query;" + selection;
                return querySuccessor(msg);
            } else if(keyHash.compareTo(myNode.getNode_id()) < 0 && keyHash.compareTo(mySuccHash) < 0 && myNode.getNode_id().compareTo(mySuccHash) > 0){
                Log.d(TAG, "queryFunc: successor node");
                msg = "Query;" + selection;
                return querySuccessor(msg);
            } else{
                Log.d(TAG, "queryFunc: next node");
                msg = "QueryNext;" + selection;
                return querySuccessor(msg);
            }
        }
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        Log.v("0 - query", selection);
        return getCursor(queryFunc(selection));
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Socket s = null;
            DataInputStream in = null;
            String str = null;
            try {
                do{
                    s = serverSocket.accept();
                    in = new DataInputStream(s.getInputStream());
                    str = in.readUTF();
                    Log.d(TAG, "Server Received string is " + str);
                    String[] messageReceived = str.split(";", 2);

                    if(messageReceived[0].equals("Join")){
                        Log.d(TAG, "doInBackground: inside Join " + messageReceived[0] + messageReceived[1]);
                        Node newNode = new Node();
                        newNode.setNode_id(genHash(messageReceived[1]));
                        newNode.setEmulator_num(messageReceived[1]);
                        newNode.setPort_num(String.valueOf((Integer.parseInt(messageReceived[1]) * 2)));
                        newNode.setPredecessor(messageReceived[1]);
                        newNode.setSuccessor(messageReceived[1]);
                        nodeList.add(newNode);

                        Collections.sort(nodeList);

                        int n = nodeList.size();
                        Log.d(TAG, "doInBackground: length of node list " + n);
                        if(n > 1){
                            for (int i = 0; i < nodeList.size(); i++) {
                                Node node = nodeList.get(i);
                                node.setPredecessor(nodeList.get((n + i - 1) % n).getPort_num());
                                node.setSuccessor(nodeList.get((n + i + 1) % n).getPort_num());
                            }
                            DataOutputStream out = new DataOutputStream(s.getOutputStream());
                            String msgToSend = newNode.getPredecessor() + ";" + newNode.getSuccessor();
                            Log.d(TAG, "doInBackground: after updating node list " + msgToSend);
                            out.writeUTF(msgToSend);
                            out.flush();
                        }
                    }else if(messageReceived[0].equals("UpdatePredecessor")){
                        Log.d(TAG, "doInBackground: in update predecessor");
                        myNode.setPredecessor(messageReceived[1]);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("received");
                        out.flush();
                        Log.d(TAG, "MyNode : "+myNode.getPredecessor()+" "+myNode.getPort_num()+" "+" "+myNode.getSuccessor());

                    }else if(messageReceived[0].equals("UpdateSuccessor")){
                        Log.d(TAG, "doInBackground: in update successor");
                        myNode.setSuccessor(messageReceived[1]);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("received");
                        out.flush();
                        Log.d(TAG, "MyNode : "+myNode.getPredecessor()+" "+myNode.getPort_num()+" "+" "+myNode.getSuccessor());
                    }else if(messageReceived[0].equals("Insert")){
                        Log.d(TAG, "doInBackground: in insert");
                        String[] values = messageReceived[1].split(";", 2);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("insert received");
                        out.flush();
                        insertValue(getContext(), values[0], values[1]);
                    } else if(messageReceived[0].equals("InsertNext")){
                        Log.d(TAG, "doInBackground: in insertNext");
                        String[] values = messageReceived[1].split(";", 2);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("insert next received");
                        out.flush();
                        insertFunc(values[0], values[1]);
                    } else if(messageReceived[0].equals("Query")){
                        Log.d(TAG, "doInBackground: in Query");
                        String cur = singleKey(messageReceived[1]);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF(cur);
                        out.flush();
                    } else if(messageReceived[0].equals("QueryNext")){
                        Log.d(TAG, "doInBackground: in Query Next");
                        String cur = queryFunc(messageReceived[1]);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF(cur);
                        out.flush();
                    } else if(messageReceived[0].equals("QueryAll")){
                        Log.d(TAG, "doInBackground: in QueryAll");
                        String[] strA = messageReceived[1].split(";");
                        String cur = null;
                        if(myNode.getSuccessor().equals(strA[0])){
                            Log.d(TAG, "doInBackground: Final Node");
                            cur = allKeys();
                        }else{
                            Log.d(TAG, "doInBackground: There is next");
                            cur = querySuccessor(str);
//                            cur = querySuccessor("QueryAll;" + strA[0] + ";" +"@");
                            cur += allKeys();
                        }
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF(cur);
                        out.flush();
                    } else if(messageReceived[0].equals("Delete")){
                        Log.d(TAG, "doInBackground: Delete");
//                        String[] values = messageReceived[1].split(";", 2);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("delete received");
                        out.flush();
                        deleteValue(messageReceived[1]);
                    } else if(messageReceived[0].equals("DeleteNext")){
                        Log.d(TAG, "doInBackground: DeleteNext");
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("delete next received");
                        out.flush();
                        deleteFunc(messageReceived[1]);
                    } else if(messageReceived[0].equals("DeleteAll")){
                        Log.d(TAG, "doInBackground: DeleteAll");

                        String[] values = messageReceived[1].split(";", 2);
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF("delete all received");
                        out.flush();
                        if(myNode.getSuccessor().equals(values[0])){
                            deleteAll();
                        }else{
                            deleteAll();
                            deleteSuccessor(str);
                        }
                    }

                    for (int i = 0; i < nodeList.size(); i++) {
                        Node node = nodeList.get(i);
                        Log.d(TAG, "nodeList : "+i+" "+node.getPredecessor()+" "+node.getPort_num()+" "+node.getNode_id()+" "+node.getSuccessor());
                    }

                }while(str != null);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "doInBackground: its io error");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }


            return null;
        }

        protected void onProgressUpdate(String...strings) {

        }
    }

    private void updatePredecessorAndSuccessor(String prePort, String succPort, String port){
        Socket preSocket = null;
        Socket succSocket = null;
        try{
            preSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(prePort));

            DataOutputStream pout = new DataOutputStream(preSocket.getOutputStream());
            pout.writeUTF("UpdateSuccessor;"+port);
            pout.flush();
            DataInputStream pin = new DataInputStream(preSocket.getInputStream());
            String recStr = pin.readUTF();
            pout.close();
            pin.close();
            preSocket.close();

            succSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(succPort));

            DataOutputStream sout = new DataOutputStream(succSocket.getOutputStream());
            sout.writeUTF("UpdatePredecessor;"+port);
            sout.flush();
            DataInputStream sin = new DataInputStream(succSocket.getInputStream());
            String recStr1 = sin.readUTF();
            sout.close();
            sin.close();
            succSocket.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Socket socket = null;
            boolean flag = true;
            try {
                String msgToSend = msgs[0];
                Log.d(TAG, "client msgToSend " + msgToSend);
                String[] a = msgToSend.split(";", 2);


                if (a[0].equals("Join")) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePorts[0]));

                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgToSend);
                    out.flush();

                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String recStr = in.readUTF();
                    Log.d(TAG, "doInBackground: string received from server " + recStr);
                    String[] rec = recStr.split(";");

                    socket.close();

                    // update current nodes predecessor and successor
                    myNode.setPredecessor(rec[0]);
                    myNode.setSuccessor(rec[1]);

                    // update predecessor node and successor node
                    updatePredecessorAndSuccessor(rec[0], rec[1], myNode.getPort_num());
                }else if(a[0].equals("Insert") || a[0].equals("InsertNext")){
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(myNode.getSuccessor()));
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgToSend);
                    out.flush();

                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String recStr = in.readUTF();
                    Log.d(TAG, "doInBackground: string received from server " + recStr);

                    socket.close();
                } else if(a[0].equals("Delete") || a[0].equals("DeleteNext")) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(myNode.getSuccessor()));
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeUTF(msgToSend);
                    out.flush();

                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String recStr = in.readUTF();
                    Log.d(TAG, "doInBackground: string received from server " + recStr);

                    socket.close();
                }

                Log.d(TAG, "doInBackground: " + "MyNode : " + myNode.getPort_num());
                Log.d(TAG, "doInBackground: " + "MyNode P: " + myNode.getPredecessor());
                Log.d(TAG, "doInBackground: " + "MyNode S: " + myNode.getSuccessor());

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
}
