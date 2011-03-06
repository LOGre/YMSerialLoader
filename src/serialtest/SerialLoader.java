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
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;
//import serialtest.emulator.SerialPortEmu;

/**
 *
 * @author Shazz, initial code by Alain M.
 *
 * Serial Loader main class
 * It uses the rxtx library and javax.comm.CommPort
 * http://download.oracle.com/docs/cd/E17802_01/products/products/javacomm/reference/api/index.html
 * http://rxtx.qbang.org/wiki/index.php/Main_Page
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
            // init the serial loader
            System.out.println("Init serial port");
            SerialLoader serialLoader = new SerialLoader();

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
    public SerialLoader() throws SerialProcessException {
        Properties props = System.getProperties();
        String jlp = props.getProperty("java.library.path");
        //props.setProperty("java.library.path", "/home/alain/tools/rxtx/rxtx-2.1-7-bins-r2/Linux/i686-unknown-linux-gnu");
        //System.setProperties(props);
        HashSet<CommPortIdentifier> portList = getAvailableSerialPorts();
        for (CommPortIdentifier cpi : portList)
        {
            System.out.println(String.format("name=%s (portType=%s) is available", cpi.getName(), cpi.getPortType()));
        }
    }

    /**
     * Retrieve available serial ports on this computer
     * @return the list of comm ports
     * @throws SerialProcessException in case of error while checking ports
     */
    public HashSet<CommPortIdentifier> getAvailableSerialPorts() throws SerialProcessException
    {
        HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        System.out.println("Check available port");
        
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
                        System.out.println(String.format("name=%s (portType=%s) is used by %s", com.getName(), com.getPortType(), com.getCurrentOwner()));
                        //throw new SerialProcessException("Port " + com.getName() + " already in use by " + com.getCurrentOwner(), e);
                    } 
                    catch (Exception e) 
                    {
                        System.out.println(String.format("name=%s (portType=%s) connot be opened", com.getName(), com.getPortType()));
                        //throw new SerialProcessException("Port " + com.getName() + " cannot be opened", e);
                    }
            }
        }
        return h;
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
            serialout.close();
            commPort.close();
        } 
        catch (IOException ex)
        {
            throw new SerialProcessException("Cannot close the stream and port", ex);
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
        if(this.commPort == null || this.serialout == null) throw new SerialProcessException("Serial Connection not set");

        //SerialPortEmu serialPortEmu = new SerialPortEmu();

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
                //serialPortEmu.receive((byte[]) (buf.get(frames)));

                // Sleep to fit the YM dump frequency (usually 50Hz)
                Thread.sleep( (int) ((1/(float) header.getFrequency()) * 700));
            }
            System.out.println("fini");
            
            // manage loop
            if(header.getLoopFrames() > 0)
            {
                while(true)
                {
                    for(int frames=header.getLoopFrames(); frames<buffer.getGetFramesNb(); frames++)
                    {
                        // send a full frame (16 registers)
                        serialout.write((byte[]) (buf.get(frames)));

                        // Sleep to fit the YM dump frequency (usually 50Hz)
                        Thread.sleep((1/header.getFrequency())*1000);
                    }
                }
            }
        }
        catch (InterruptedException ex)
        {
            throw new SerialProcessException(ex);
        }
        catch(IOException ex)
        {
            throw new SerialProcessException(ex);
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
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

            if (portIdentifier.isCurrentlyOwned())
            {
                throw new SerialProcessException("The port " + portName + " is currently in use");
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
                    throw new SerialProcessException("Only serial ports are handled.");
                }
            }
        }
        catch (IOException ex)
        {
           throw new SerialProcessException("Cannot get output stream", ex);
        }
        catch (UnsupportedCommOperationException ex)
        {
            throw new SerialProcessException("Unssuported operation", ex);
        }
        catch (PortInUseException ex)
        {
            throw new SerialProcessException("The port " + portName + " is already in use", ex);
        }
        catch(gnu.io.NoSuchPortException ex)
        {
            throw new SerialProcessException("The port " + portName + " doesn't exist", ex);
        }
    }
}
