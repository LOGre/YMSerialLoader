/*
 * This Serial loader is resposnible of :
 * - Depacking a YM dump file (see specs : ftp://ftp.modland.com/pub/documents/format_documentation/Atari%20ST%20Sound%20Chip%20Emulator%20YM1-6%20(.ay,%20.ym).txt)
 * - Extracting the YM registers frames from the dump
 * - Streaming the frames thru the serial port at the good frequency (UART frequency AND YM dump frequency)
 * - the YM papilio core should play some cool YM2149 music
 * 
 */
package serialtest;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

/**
 *
 * @author Shazz, initial code by Alain M.
 *
 * Serial Loader main class
 * It uses the rxtx library and javax.comm.CommPort
 * http://download.oracle.com/docs/cd/E17802_01/products/products/javacomm/reference/api/index.html
 * 
 */
public class SerialLoader {


    private OutputStream serialout;
    private CommPort commPort;


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
            System.out.println("Depacking : " + fileToDepack);

            // Depack and display header & dump on screen
            YMLoader loader = new YMLoader();
            loader.depack(fileToDepack);
            YMHeader header = loader.decodeFileFormat();
            loader.dump();

            // init the serial loader
            System.out.println("Sending on port : " + port + " at " + uartFreq + " Hz");
            SerialLoader serialLoader = new SerialLoader();

            // connect the port at the good frequency
            serialLoader.connect(port, uartFreq);

            // stream the data to the serial port
            serialLoader.stream(loader.getFramesBuffer(), header.getFrequency());

            // disconnect the port
            serialLoader.disconnect();

            //bye bye
            System.out.println("Done, exiting");
        }
        catch(Exception ex)
        {
            System.err.println("FATAL ERROR: " + ex.getMessage());
            //ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructor
     * 
     * @throws Exception
     */
    public SerialLoader() throws Exception {
        Properties props = System.getProperties();
        String jlp = props.getProperty("java.library.path");
        //props.setProperty("java.library.path", "/home/alain/tools/rxtx/rxtx-2.1-7-bins-r2/Linux/i686-unknown-linux-gnu");
        //System.setProperties(props);
        HashSet<CommPortIdentifier> portList = getAvailableSerialPorts();
        for (CommPortIdentifier i : portList)
        {
            dump(i);
        }
    }

    /**
     * Retrieve available serial ports on this computer
     * @return the list of comm ports
     * @throws Exception in case of error while checking ports
     */
    public HashSet<CommPortIdentifier> getAvailableSerialPorts() throws Exception
    {
        HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();

        while (thePorts.hasMoreElements())
        {
            CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();

            switch (com.getPortType())
            {
                case CommPortIdentifier.PORT_SERIAL:
                    try
                    {
                        CommPort thePort = com.open("CommUtil", 50);
                        thePort.close();
                        h.add(com);
                    }
                    catch (PortInUseException e)
                    {
                        throw new Exception("Port " + com.getName() + " already in use by " + com.getCurrentOwner());
                    } 
                    catch (Exception e) 
                    {
                        throw new Exception("Port " + com.getName() + " cannot be opened");
                    }
            }
        }
        return h;
    }

    /**
     * Dump the port info
     */
    private void dump(CommPortIdentifier cpi) {
        String str = String.format("owner=%s\tname=%s\tportType=%s\t", cpi.getCurrentOwner(), cpi.getName(), cpi.getPortType());
        System.out.println(str);

    }

    /**
     * Disconnect the serial port
     * @throws Exception
     */
    public void disconnect() throws Exception
    {
        try
        {
            // close everything
            serialout.close();
            commPort.close();
        } 
        catch (IOException ex)
        {
            throw new Exception(ex);
        }
        
    }

    /**
     * Stream a YMBuffer thru the serial port according to the dump frequency
     * @param buffer
     * @param frequency
     * @throws Exception
     */
    public void stream(YMFramesBuffer buffer, int frequency) throws Exception
    {
        if(this.commPort == null || this.serialout == null) throw new Exception("Serial Connection not set");
        
        try
        {
            Vector buf = buffer.getFramesData();
            for(int frames=0; frames<buffer.getGetFramesNb(); frames++)
            {
                // send a full frame (16 registers)
                /*
                 *        -------------------------------------------------------
                 *              b7 b6 b5 b4 b3 b2 b1 b0
                 *         r0:  X  X  X  X  X  X  X  X   Period voice A
                 *         r1:  -  -  -  -  X  X  X  X   Period voice A
                 *         r2:  X  X  X  X  X  X  X  X   Period voice B
                 *         r3:  -  -  -  -  X  X  X  X   Period voice B
                 *         r4:  X  X  X  X  X  X  X  X   Period voice C
                 *         r5:  -  -  -  -  X  X  X  X   Period voice C
                 *         r6:  -  -  -  X  X  X  X  X   Noise period
                 *         r7:  X  X  X  X  X  X  X  X   Mixer control
                 *         r8:  -  -  -  X  X  X  X  X   Volume voice A
                 *         r9:  -  -  -  X  X  X  X  X   Volume voice B
                 *        r10:  -  -  -  X  X  X  X  X   Volume voice C
                 *        r11:  X  X  X  X  X  X  X  X   Waveform period
                 *        r12:  X  X  X  X  X  X  X  X   Waveform period
                 *        r13:  -  -  -  -  X  X  X  X   Waveform shape
                 *        -------------------------------------------------------
                 *        New "virtual" registers to store extra data:
                 *        -------------------------------------------------------
                 *        r14:  -  -  -  -  -  -  -  -   Frequency for DD1 or TS1
                 *        r15:  -  -  -  -  -  -  -  -   Frequency for DD2 or TS2
                 *
                 */

                serialout.write((byte[]) (buf.get(frames)));

                // Sleep to fit the YM dump frequency (usually 50Hz)
                Thread.sleep((1/frequency)*1000);
            }
        }
        catch(IOException ex)
        {
            throw new Exception(ex);
        }
    }

    /**
     * Connect to the serial port at the given baud rate
     * @param portName
     * @param uartFreq
     * @throws Exception
     */
    public void connect(String portName, int uartFreq) throws Exception
    {
        try
        {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

            if (portIdentifier.isCurrentlyOwned())
            {
                throw new Exception("Port is currently in use");
            }
            else
            {
                commPort = portIdentifier.open(this.getClass().getName(), 2000);

                if (commPort instanceof SerialPort)
                {
                    SerialPort serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(uartFreq, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    serialout = serialPort.getOutputStream();

                    System.out.println("Connected to port " + portName + " at " + uartFreq + " bauds");
                }
                else
                {
                    throw new Exception("Only serial ports are handled.");
                }
            }
        }
        catch(gnu.io.NoSuchPortException ex)
        {
            throw new Exception("This port doesn't exist");
        }
    }
}
