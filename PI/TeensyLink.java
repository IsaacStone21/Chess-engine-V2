package PI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fazecast.jSerialComm.SerialPort;



public class TeensyLink {

    public static void main(String[] args) throws IOException, InterruptedException {

        TeensyLink teensy = new TeensyLink("COM5");

        teensy.ping("GreenLight");

        teensy.close();
    }

    
    private String teensyName;
    private SerialPort port;

    public TeensyLink(String teensyName) throws IOException {
        this.teensyName = teensyName;
        init();
    }

    private void init() throws IOException {

        port = SerialPort.getCommPort(teensyName);

        if (!port.openPort()) {
            throw new IOException("Couldn't open port: " + teensyName);
        }
    }
    

    public void close() {
        port.closePort();
    }


    private String sendCommand(String[] args) throws IOException, InterruptedException {

        String command = String.join(" ", args) + '\n';

        byte[] bytes = command.getBytes(StandardCharsets.US_ASCII);

        int bytesSent = port.writeBytes(bytes, bytes.length);

        if(bytesSent != bytes.length) {
            throw new IOException("Error sending command: " + command.strip());
        }

        Thread.sleep(200);

        if(args[0].equals("INIT") || args[0].equals("MOVE")) {
            Thread.sleep(7000);
        }

        byte[] response = new byte[port.bytesAvailable()];

        port.readBytes(response, response.length);

        String feedback = new String(response, StandardCharsets.US_ASCII);

        return feedback;
    }


    public String ping(String token) throws IOException, InterruptedException {

        String[] args = {"PING", token};
        return sendCommand(args);
    }


    public String scan() throws IOException, InterruptedException {
        String[] args = {"SCAN"};
        return sendCommand(args);
    }

    public String initialize() throws InterruptedException, IOException{
        String[] args = {"INIT"};
        return sendCommand(args);
    }

    public String move(String[] args) throws IOException, InterruptedException {
        String[] args2 = new String[args.length + 1];
        args2[0] = "MOVE";
        for(int i = 1; i < args2.length; i++) {
            args2[i] = args[i - 1];
        }

        return sendCommand(args2);
    }

    public static void listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();

        for(int i = 0; i < ports.length; i++) {
            System.out.print(ports[i].getDescriptivePortName() + ": ");
            System.out.println(ports[i].getSystemPortName());
        }
    }
    
}
