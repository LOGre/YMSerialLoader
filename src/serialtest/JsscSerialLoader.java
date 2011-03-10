/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package serialtest;


import java.util.Vector;
import jssc.*;

/**
 *
 * @author admin
 */
public class JsscSerialLoader
{
    private SerialPort serialPort;


    /**
     * Main class, startup
     * @param args
     */
    public static void main(String[] args)
    {
        // Check the passed arguments
        String fileToDepack = "";
        String port = "";
        int uartFreq = 0;
        if (args.length == 3)
        {
            fileToDepack = args[0];
            port = args[1];
            try
            {
                uartFreq = Integer.parseInt(args[2]);
            }
            catch (NumberFormatException e) {
                System.err.println("Argument must be an integer");
                System.err.println("Usage : java SerialTest.SerialLoader file port frequency");
                System.exit(1);
            }
        }
        else
        {
            System.err.println("Wrong arguments");
            System.err.println("Usage : java SerialLoader file port frequency");
            System.exit(1);
        }

        // Arguments parsed, let's depack and stream the YM dump now
        try
        {
            // init the serial loader
            System.out.println("Init serial port");
            JsscSerialLoader serialLoader = new JsscSerialLoader();

            // connect the port at the good frequency
            System.out.println("Connect serial port : " + port + " at " + uartFreq + " Hz");
            serialLoader.connect(port, uartFreq);

            // Depack and display header & dump on screen
            System.out.println("Depacking : " + fileToDepack);
            YMLoader loader = new YMLoader();
            loader.depack(fileToDepack);
            YMHeader header = loader.decodeFileFormat();
            loader.dump();

            // stream the data to the serial port
            System.out.println("Sending on port : " + port + " at " + uartFreq + " Hz");
            serialLoader.stream(loader.getFramesBuffer(), header);

            // disconnect the port
            System.out.println("Stream ended, disconnecting...");
            serialLoader.disconnect();

            //bye bye
            System.out.println("Done, exiting");
        }
        catch(YMProcessException ex)
        {
            System.err.println("FATAL : " + ex.getMessage());
            System.exit(1);
        }
        catch(SerialProcessException ex)
        {
            System.err.println("FATAL : " + ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Constructor
     *
     * @throws SerialProcessException
     */
    public JsscSerialLoader() throws SerialProcessException
    {
        //Properties props = System.getProperties();
        //String jlp = props.getProperty("java.library.path");
        //props.setProperty("java.library.path", "/home/alain/tools/rxtx/rxtx-2.1-7-bins-r2/Linux/i686-unknown-linux-gnu");
        //System.setProperties(props);
        System.out.println("Check available port");
        String[] portNames = SerialPortList.getPortNames();
        for(int i = 0; i < portNames.length; i++){
            System.out.println(portNames[i]);
        }

    }


    /**
     * Disconnect the serial port
     * @throws SerialProcessException
     */
    public void disconnect() throws SerialProcessException
    {
        try
        {
            // close everything
            serialPort.closePort();
        }
        catch (SerialPortException ex)
        {
            throw new SerialProcessException("Cannot close the serial port", ex);
        }       
    }

    /**
     * Stream a YMBuffer thru the serial port according to the dump frequency
     * @param buffer
     * @param frequency
     * @throws SerialProcessException
     */
    public void stream(YMFramesBuffer buffer, YMHeader header) throws SerialProcessException
    {
        if(this.serialPort == null) throw new SerialProcessException("Serial Connection not set");

        //SerialPortEmu serialPortEmu = new SerialPortEmu();

        try
        {
            Vector buf = buffer.getFramesData();
            for(int frames=0; frames<buffer.getGetFramesNb(); frames++)
            {
                // send a full frame (16 registers)
                byte[] regs = (byte[]) (buf.get(frames));
                serialPort.writeBytes(regs);

                // Sleep to fit the YM dump frequency (usually 50Hz)
                //Thread.sleep( (int) ((1/(float) header.getFrequency())));
            }
            System.out.println("end before loop");

            // manage loop
            if(header.getLoopFrames() > 0)
            {
                while(true)
                {
                    for(int frames=header.getLoopFrames(); frames<buffer.getGetFramesNb(); frames++)
                    {
                        // send a full frame (16 registers)
                        byte[] regs = (byte[]) (buf.get(frames));
                        serialPort.writeBytes(regs);

                        // Sleep to fit the YM dump frequency (usually 50Hz)
                        //Thread.sleep((1/header.getFrequency())*200);
                    }
                }
            }
        }
        catch (SerialPortException ex)
        {
            throw new SerialProcessException(ex.getMessage(), ex);
        }
    }

    /**
     * Connect to the serial port at the given baud rate
     * @param portName
     * @param uartFreq
     * @throws SerialProcessException
     */
    public void connect(String portName, int uartFreq) throws SerialProcessException
    {     
        try
        {
            serialPort = new SerialPort(portName);
            serialPort.openPort();
            serialPort.setParams(uartFreq,
                                 SerialPort.DATABITS_8,
                                 SerialPort.STOPBITS_1,
                                 SerialPort.PARITY_NONE);

            System.out.println("Connected to port " + portName + " at " + uartFreq + " bauds");

        }
        catch (SerialPortException ex)
        {
            throw new SerialProcessException(ex.getMessage(), ex);
        }
    }
}
