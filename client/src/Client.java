import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;

import java.io.*;
import java.net.DatagramPacket;
import java.net.Socket;

public class Client {
    private int mServerPort;
    private MapUI mUI;
    private GraphHopper mHopper;
    private String mServerIP;

    public Client(String serverIP, int serverPort, String pbfPath, String ghPath, String mapPath) {
        mServerPort = serverPort;
        mUI = new MapUI(mapPath, "Client");
        EncodingManager em = new EncodingManager("car");
        mHopper = new GraphHopperOSM()
                        .setOSMFile(pbfPath)
                        .forDesktop()
                        .setGraphHopperLocation(ghPath)
                        .setEncodingManager(em)
                        .importOrLoad();
        mServerIP = serverIP;
    }

    private void onePath(Pair<Double, Double> startPoint, Pair<Double, Double> endPoint, AdjacencyList<Integer> adList) {

    }

    /**
     * Compute the route and visualize it.
     * **Note** currently doesn't have random shift
     * @param startPoint The start point
     * @param endPoint The end point
     */
    public void compute(Pair<Double, Double> startPoint, Pair<Double, Double> endPoint) throws Exception{
        /*TODO: add random shift*/
        Pair<Double, Double> shiftStart = startPoint;
        Pair<Double, Double> shiftEnd = endPoint;

        Socket client = new Socket(mServerIP, mServerPort);
        OutputStream socketOut = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(socketOut);

        out.writeDouble(shiftStart.mFirst);
        out.writeDouble(shiftStart.mSecond);
        out.writeDouble(shiftEnd.mFirst);
        out.writeDouble(shiftEnd.mSecond);

        InputStream socketIn = client.getInputStream();
        DataInputStream in = new DataInputStream(socketIn);

        ObjectInputStream oIn = new ObjectInputStream(in);
        Reply reply = (Reply) oIn.readObject();
        oIn.close();
        in.close();
        out.close();

    }
}
