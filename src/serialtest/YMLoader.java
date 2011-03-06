/*
 * The YMLoader class
 * Based on YM format description by Leonard / Oxygene
 * http://leonard.oxg.free.fr/ymformat.html
 * ftp://ftp.modland.com/pub/documents/format_documentation/Atari%20ST%20Sound%20Chip%20Emulator%20YM1-6%20(.ay,%20.ym).txt
 */
package serialtest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.sourceforge.lhadecompressor.LhaEntry;
import net.sourceforge.lhadecompressor.LhaException;
import net.sourceforge.lhadecompressor.LhaFile;

/**
 * YM Loader
 * @author shazz
 */
public class YMLoader {

    final private static int BUFFSER_SIZE = 4096;
    private ByteBuffer buffer;
    private YMHeader header;
    private YMDigidrum[] digidrumsTable;

    private byte[][] framesData;

    private boolean alreadyDecoded = false;

    /**
     * Get the Digidrums 
     * @return Digidrums
     */
    public YMDigidrum[] getDigidrumsTable() {
        return digidrumsTable;
    }

    /**
     * Get the Header
     * @return the header
     */
    public YMHeader getHeader() {
        return header;
    }

    /**
     * Depack the YM file (LHA compression)
     * @param filename
     * @throws YMProcessException
     */
    public void depack(String filename) throws YMProcessException
    {
        try 
        {
            // reset the stuff if called 2 times
            alreadyDecoded = false;
            buffer = null;
            header = null;
            digidrumsTable = null;
            framesData = null;
            
            byte[] buff = new byte[BUFFSER_SIZE];
            LhaFile lhafile = new LhaFile(filename);
            LhaEntry entry = lhafile.getEntry(0);
            System.out.println("    EXTRACT FILE    = " + entry.getFile());
            System.out.println("    METHOD          = " + entry.getMethod());
            System.out.println("    COMPRESSED SIZE = " + entry.getCompressedSize());
            System.out.println("    ORIGINAL SIZE   = " + entry.getOriginalSize());
            System.out.println("    TIME STAMP      = " + entry.getTimeStamp());
            System.out.println("    OS ID           = " + (char) entry.getOS());
            InputStream in = new BufferedInputStream(lhafile.getInputStream(entry), BUFFSER_SIZE);
            ByteArrayOutputStream bastream = new ByteArrayOutputStream((int) entry.getOriginalSize());
            int len = 0;
            while (true) {
                len = in.read(buff, 0, BUFFSER_SIZE);
                if (len < 0) {
                    break;
                }
                if (len < BUFFSER_SIZE) {
                    bastream.write(buff, 0, len);
                } else {
                    bastream.write(buff);
                }
            }
            bastream.flush();
            buffer = ByteBuffer.wrap(bastream.toByteArray());

            // WARNING: All DWORD or WORD are stored in MOTOROLA order in the file (INTEL reverse)
            buffer.order(ByteOrder.BIG_ENDIAN);
            
            bastream.close();
            lhafile.close();
        }
        catch (LhaException ex)
        {
            throw new YMProcessException(ex);
        }
        catch (IOException ex)
        {
            throw new YMProcessException(ex);
        }

    }

    /**
     * Decode the YM file format
     * @return the YM Header
     * @throws YMProcessException
     */
    public YMHeader decodeFileFormat() throws YMProcessException
    {
        if(buffer == null) throw new YMProcessException("YM not depacked yet");
        if(alreadyDecoded) return this.header;

        try
        {
            header = new YMHeader();

            // Read ID
            header.setId(getString(4));
            checkHeader(header.getId());

            if(header.getId().equals(YMHeader.YM2)) throw new YMProcessException("Format " + YMHeader.YM2 + " not yet supported");
            if(header.getId().equals(YMHeader.MIX1)) throw new YMProcessException("Format " + YMHeader.MIX1 + " not yet supported");
            if(header.getId().equals(YMHeader.YMT2)) throw new YMProcessException("Format " + YMHeader.YMT2 + " not yet supported");

            if(header.getId().equals(YMHeader.YM3)) decodeYM3Format(header, false);
            else if(header.getId().equals(YMHeader.YM3)) decodeYM3Format(header, false);
            else if(header.getId().equals(YMHeader.YM3b)) decodeYM3Format(header, true);
            else if(header.getId().equals(YMHeader.YM5)) decodeYM5Format(header);
            else if(header.getId().equals(YMHeader.YM6)) decodeYM6Format(header);
            else if(header.getId().equals(YMHeader.YM4)) decodeYM4Format(header);

            alreadyDecoded = true;
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            throw new YMProcessException("Error while parsing the file : " + ex.getMessage(), ex);
        }


        return header;
    }

    private void decodeYM6Format(YMHeader header) throws YMProcessException
    {
        decodeYM5Format(header);
    }

    private void decodeYM4Format(YMHeader header) throws YMProcessException
    {
        if(header == null) throw new YMProcessException("header not instanced");

        // Read signature
        header.setLeo(getString(8));
        checkSignature(header.getLeo());

        // Read dumped frames nb
        header.setFrames(buffer.getInt());

        // Read attributes
        int songAttr = buffer.getInt();
        header.setInterleaved((songAttr&0x01)==1);
        header.setDigiDrumsSignedSamples(((songAttr >> 1)&0x01)==1);
        header.setDigiDrums4bitsSTFormat(((songAttr >> 2)&0x01)==1);
        header.setDigidrums(buffer.getShort());

        // Read loop point if set
        header.setLoopFrames(buffer.getInt());

        // Read digidrums data if set
        if(header.getDigidrums() > 0)
        {
            digidrumsTable = new YMDigidrum[header.getDigidrums()];
            for(int i=0; i<header.getDigidrums();i++)
            {
                digidrumsTable[i] = new YMDigidrum();
                digidrumsTable[i].setDigidrumId(i);

                int size = buffer.getInt();
                digidrumsTable[i].setSampleSize(size);

                System.out.println("Sample " + i + " is size " + size);
                digidrumsTable[i].setSample(getByte(size));
            }
        }

        // Read song info
        header.setSongName(getStringNT());
        header.setAuthorName(getStringNT());
        header.setSongComment(getStringNT());

        // get frames data, if interleaved, transpose the table
        framesData = new byte[header.getFrames()][16];
        if(header.isInterleaved())
        {
            byte[][] transposedData = new byte[16][header.getFrames()];
            for(int reg=0;reg<16;reg++)
            {
                for(int frames=0;frames<header.getFrames();frames++)
                {
                    transposedData[reg][frames] = buffer.get();
                }
            }
            // transpose data
            for(int reg=0;reg<16;reg++)
            {
                for(int frames=0;frames<header.getFrames();frames++)
                {
                    framesData[frames][reg] = transposedData[reg][frames];
                }
            }
        }
        else
        {
           for(int frames=0;frames<header.getFrames();frames++)
           {
                for(int reg=0;reg<16;reg++)
                {
                    framesData[frames][reg] = buffer.get();
                }
            }
        }

        // check end tag
        String eof = getEndString();
        if(eof.matches("End!")) System.out.println("End of file found");
        else System.out.println("End : " + eof);
    }

    private void decodeYM5Format(YMHeader header) throws YMProcessException
    {
        if(header == null) throw new YMProcessException("header not instanced");

        // Read signature
        header.setLeo(getString(8));
        checkSignature(header.getLeo());

        // Read dumped frames nb
        header.setFrames(buffer.getInt());

        // Read attributes
        int songAttr = buffer.getInt();
        header.setInterleaved((songAttr&0x01)==1);
        header.setDigiDrumsSignedSamples(((songAttr >> 1)&0x01)==1);
        header.setDigiDrums4bitsSTFormat(((songAttr >> 2)&0x01)==1);
        header.setDigidrums(buffer.getShort());

        // only YM5 and YM6 formats handles the frequency stuff
        header.setClock(buffer.getInt());
        header.setFrequency(buffer.getShort());

        // Read loop point if set
        header.setLoopFrames(buffer.getInt());

        // only YM5 and YM6 formats handles the future data stuff
        header.setFuturDataSize(buffer.getShort());

        // Read digidrums data if set
        if(header.getDigidrums() > 0)
        {
            digidrumsTable = new YMDigidrum[header.getDigidrums()];
            for(int i=0; i<header.getDigidrums();i++)
            {
                digidrumsTable[i] = new YMDigidrum();
                digidrumsTable[i].setDigidrumId(i);

                int size = buffer.getInt();
                digidrumsTable[i].setSampleSize(size);

                System.out.println("Sample " + i + " is size " + size);
                digidrumsTable[i].setSample(getByte(size));
            }
        }

        // Read song info
        header.setSongName(getStringNT());
        header.setAuthorName(getStringNT());
        header.setSongComment(getStringNT());

        // get frames data, if interleaved, transpose the table
        framesData = new byte[header.getFrames()][16];
        if(header.isInterleaved())
        {
            byte[][] transposedData = new byte[16][header.getFrames()];
            for(int reg=0;reg<16;reg++)
            {
                for(int frames=0;frames<header.getFrames();frames++)
                {
                    transposedData[reg][frames] = buffer.get();
                }
            }
            // transpose data
            for(int reg=0;reg<16;reg++)
            {
                for(int frames=0;frames<header.getFrames();frames++)
                {
                    framesData[frames][reg] = transposedData[reg][frames];
                }
            }
        }
        else
        {
           for(int frames=0;frames<header.getFrames();frames++)
           {
                for(int reg=0;reg<16;reg++)
                {
                    framesData[frames][reg] = buffer.get();
                }
            }
        }

        // check end tag
        String eof = getEndString();
        if(eof.matches("End!")) System.out.println("End of file found");
        else System.out.println("End : " + eof);

    }

    private void decodeYM3Format(YMHeader header, boolean loopSupport) throws YMProcessException
    {
        if(header == null) throw new YMProcessException("header not instanced");

        // get frames data, nvbl = (ymfile_size-4)/14;
        int nbframes = (buffer.capacity()-4)/14;
        header.setFrames(nbframes);

        framesData = new byte[header.getFrames()][16];
        for(int frames=0;frames<nbframes;frames++)
        {
            for(int reg=0;reg<14;reg++)
            {
                framesData[frames][reg] = buffer.get();
            }

            // set to 0 special effects registers
            framesData[frames][14] = 0;
            framesData[frames][15] = 0;
        }

        if(loopSupport)
        {
            //header.setLoopFrames(getMotoInt());
            header.setLoopFrames(buffer.getInt());
        }
    }

    /**
     * dump the YM frames to screen
     */
    public void dump()
    {
        header.dump();

        if(header.getDigidrums() > 0)
        {
            for(int d=0; d<digidrumsTable.length; d++)
            {
                digidrumsTable[d].dump();
            }
        }

        /*int nbFrames = header.getFrames();

        System.out.println("Registers   00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F");
        System.out.println("-----------------------------------------------------------");
        for(int i=0;i<nbFrames;i++)
        {
            System.out.print("Frame " + (Integer.toHexString(0x1000 | i )).substring(1).toUpperCase() + " : ");
            for(int j=0;j<16;j++)
            {
                System.out.print(Integer.toHexString(0x100 | (framesData[i][j] & 0xFF )).substring(1).toUpperCase() + " ");
            }
            System.out.println("");
        }*/

    }

    /**
     * Retrieve the frames
     * @return a YMFramesBuffer
     */
    public YMFramesBuffer getFramesBuffer()
    {
        return new YMFramesBuffer(framesData, header.getFrames(), 16);
    }

    /**
     * Util to read n char and build a java string from a Bytebuffer
     * @param n
     * @return the string
     */
    private String getString(int n)
    {
        String res = "";
        for(int i=0;i<n;i++)
        {
            res += (new Character((char) (buffer.get()))).toString();
        }

        return res;
    }

    /**
     * Util to read a null terminated string from the bytebuffer
     * @param n
     * @return the NT String
     */
    private String getStringNT()
    {
        String res = "";
        byte aByte = buffer.get();
        while(aByte != 0)
        {
            res += new Character((char) (aByte)).toString();
            aByte = buffer.get();
        }

        return res;
    }

    /**
     * Util to read a null terminated string from the bytebuffer
     * @param n
     * @return the NT String
     */
    private String getStringCT(char c)
    {
        String res = "";
        byte aByte = buffer.get();
        while(aByte != c)
        {
            res += new Character((char) (aByte)).toString();
            aByte = buffer.get();
        }

        return res;
    }

    /**
     * Util to read un til eof of buffer
     * @param n
     * @return the String
     */
    private String getEndString()
    {
        String res = "";
        try
        {
            while(true)
            {
                res += new Character((char) buffer.get()).toString();
            }
        }
        catch(java.nio.BufferUnderflowException ex)
        {
            // ok
        }

        return res;
    }

    /**
     * Util to read n bytes
     * @param n
     * @return the bytes
     */
    private byte[] getByte(int n)
    {
        byte[] res = new byte[n];
        for(int i=0;i<n;i++)
        {
            res[i] = buffer.get();
        }

        return res;
    }

    /**
     * Util to read n integer
     * @param n
     * @return the int
     */
    private int getInt(int n)
    {
        int res = 0;
        for(int i=0;i<n;i++)
        {
            res = (res << 8) + (int) (buffer.get() & 0xFF);
        }

        return  res;
    }

    /**
     * Util to read Motorola Long Word
     * @return the int in reverse byte order
     */
    private int getMotoInt()
    {
            byte[] aByte = getByte(4);
            return (((aByte[3] & 0xFF) << 24) | ((aByte[2] & 0xFF) << 16) | ((aByte[1] & 0xFF) << 8) | ((aByte[0] & 0xFF)));
    }

    /**
     * Check header validity
     * @throws YMProcessException
     */
    private void checkHeader(String id) throws YMProcessException
    {
            boolean found = false;
            for(int i=0; i<YMHeader.idtags.length; i++)
            {
                if(id.equals(YMHeader.idtags[i]))
                {
                    found = true;
                    break;
                }
            }

            if(!found) throw new YMProcessException("ID tag not recognized");   
    }

    /**
     * Check the signature
     * @throws YMProcessException
     */
    private void checkSignature(String leo) throws YMProcessException
    {
        if(!leo.equals(YMHeader.SIGNATURE)) throw new YMProcessException("Signature doesn't match");
    }
}
