/*
 * YMFramesBuffer class
 */

package serialtest;

import java.util.Vector;

/**
 *
 * @author shazz
 */
public class YMFramesBuffer
{
    private Vector framesData;
    private int framesNb;
    private int registersNb;

    /**
     * Constructor
     * @param data
     * @param framesNb
     * @param registersNb
     */
    public YMFramesBuffer(byte[][] data, int framesNb, int registersNb)
    {
        this.framesData = new Vector();
        
        for(int frames=0; frames<framesNb; frames++)
        {
            this.framesData.add(data[frames]);
        }
        this.framesNb = framesNb;
        this.registersNb = registersNb;
    }

    /**
     * Get the list of frames data
     * @return the list of frames data
     */
    public Vector getFramesData() {
        return framesData;
    }

    /**
     * Get the number of frames available
     * @return the number of frames available
     */
    public int getGetFramesNb() {
        return framesNb;
    }

    /**
     * Get the number of registers per frame
     * @return the number of registers per frame
     */
    public int getGetRegistersNb() {
        return registersNb;
    }

    
}
