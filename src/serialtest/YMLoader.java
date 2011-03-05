/*
 * The YMLoader class
 */
package serialtest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import net.sourceforge.lhadecompressor.LhaEntry;
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

    /**
     * Depack the YM file (LHA compression)
     * @param filename
     * @throws IOException
     */
    public void depack(String filename) throws IOException {
        
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
        while (true)
        {
            len = in.read(buff, 0, BUFFSER_SIZE);
            if(len < 0) break;

            if(len < BUFFSER_SIZE)
                bastream.write(buff, 0, len);
            else
                bastream.write(buff);
                
        }
        bastream.flush();
        buffer = ByteBuffer.wrap(bastream.toByteArray());

        bastream.close();
        lhafile.close();

    }

    /**
     * Decode the YM file format
     * @return
     * @throws Exception
     */
    public YMHeader decodeFileFormat() throws Exception
    {
        if(buffer == null) throw new Exception("YM not depacked yet");

        header = new YMHeader();
        header.setId(getString(4));
        header.setLeo(getString(8));
        header.setFrames(buffer.getInt());

        int songAttr = buffer.getInt();
        header.setInterleaved((songAttr&0x01)==1);
        header.setDigiDrumsSignedSamples(((songAttr >> 1)&0x01)==1);
        header.setDigiDrums4bitsSTFormat(((songAttr >> 2)&0x01)==1);
        header.setDigidrums(buffer.getShort());
        header.setClock(buffer.getInt());
        header.setFrequency(buffer.getShort());
        header.setLoopFrames(buffer.getInt());
        header.setFuturDataSize(buffer.getShort());

        digidrumsTable = new YMDigidrum[header.getDigidrums()];
        for(int i=0; i<header.getDigidrums();i++)
        {
            digidrumsTable[i] = new YMDigidrum();
            int size = buffer.getInt();
            digidrumsTable[i].setSampleSize(size);
            
            System.out.println("Sample " + i + " is size " + size);
            digidrumsTable[i].setSample(getByte(size));
        }
        header.setSongName(getStringNT());
        header.setAuthorName(getStringNT());
        header.setSongComment(getStringNT());

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

        return header;
    }

    /**
     * dump the YM frames to screen
     */
    public void dump()
    {
        header.dump();
        int nbFrames = header.getFrames();

        System.out.println("r1\tr2\tr3\tr4\tr5\tr6\tr7\tr8\tr9\tr10\tr11\tr12\tr13\tr14\tr15\tr16");
        for(int i=0;i<nbFrames;i++)
        {
            for(int j=0;j<16;j++)
            {
                System.out.print((framesData[i][j] & 0xFF )+ "\t");
            }
            System.out.println("");
        }

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
}
