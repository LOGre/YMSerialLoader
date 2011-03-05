/*
 * YMDigidrum Class
 */

package serialtest;

/**
 * This class is there to model the embedded digidrums samples
 * @author shazz
 */
public class YMDigidrum {

    private int sampleSize;
    byte[] sample;

    /**
     * Constructor
     */
    public YMDigidrum() {
    }

    /**
     * Get the digidrum sample
     * @return the sample byte array
     */
    public byte[] getSample() {
        return sample;
    }

    /**
     * Set the digidrum sample
     * @param sample
     */
    public void setSample(byte[] sample) {
        this.sample = sample;
    }

    /**
     * Get the digidrum sample size
     * @return the sample size in bytes
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Set the digidrum sample size
     * @param sampleSize
     */
    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    

}
