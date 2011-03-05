/*
 * YM Header class
 */

package serialtest;

/**
 *
 * @author admin
 */
public class YMHeader
{
    private String id;
    private String leo;
    private int frames;
    private boolean interleaved;
    private boolean digiDrumsSignedSamples;
    private boolean digiDrums4bitsSTFormat;
    private short digidrums;
    private int clock;
    private short frequency;
    private int loopFrames;
    private int futurDataSize;

    private String songName;
    private String authorName;
    private String songComment;


    /**
     * Constructor
     */
    public YMHeader() {

    }

    /**
     * dump the YM header
     */
    public void dump()
    {
        System.out.println("---------------------------------------------------");
        System.out.println("YM version : " + getId());
        System.out.println("Code by : " + getLeo());

        int minutes = Math.abs(getFrames()/getFrequency()/60);
        int sec = Math.abs(getFrames()/getFrequency() - (minutes*60));
        System.out.println("Frames dumped : " + getFrames() + " (" + minutes + " min " + sec + " sec)");
        if(isInterleaved()) System.out.println("Interleaved : Yes");
        else System.out.println("Interleaved : No");

        if(getDigidrums() > 0)
        {
            System.out.print(getDigidrums() + " digidrums which are ");
            if(!isDigiDrumsSignedSamples()) System.out.print("not ");
            System.out.print("signed and ");
            if(!isDigiDrums4bitsSTFormat()) System.out.print("not ");
            System.out.println("in ST format");
        }
        System.out.println("YM clock : " + getClock() + " Hz");
        System.out.println("Replay frequency : " + getFrequency() + " Hz");
        
        if(getLoopFrames() > 0)  System.out.println("Loop after " + getLoopFrames() + " frames");
        System.out.println("Unused : " + getFuturDataSize());

        System.out.println("Song : " + getSongName());
        System.out.println("Author : " + getAuthorName());
        System.out.println("Comments : " + getSongComment());
        System.out.println("---------------------------------------------------");
    }

    /**
     *
     * @return
     */
    public boolean isDigiDrums4bitsSTFormat() {
        return digiDrums4bitsSTFormat;
    }

    /**
     *
     * @param areDigiDrums4bitsSTFormat
     */
    public void setDigiDrums4bitsSTFormat(boolean areDigiDrums4bitsSTFormat) {
        this.digiDrums4bitsSTFormat = areDigiDrums4bitsSTFormat;
    }

    /**
     *
     * @return
     */
    public boolean isDigiDrumsSignedSamples() {
        return digiDrumsSignedSamples;
    }

    /**
     *
     * @param areDigiDrumsSignedSamples
     */
    public void setDigiDrumsSignedSamples(boolean areDigiDrumsSignedSamples) {
        this.digiDrumsSignedSamples = areDigiDrumsSignedSamples;
    }

    /**
     *
     * @return
     */
    public boolean isInterleaved() {
        return interleaved;
    }

    /**
     *
     * @param isInterleaved
     */
    public void setInterleaved(boolean isInterleaved) {
        this.interleaved = isInterleaved;
    }



    /**
     *
     * @return
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     *
     * @param authorName
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     *
     * @return
     */
    public String getSongComment() {
        return songComment;
    }

    /**
     *
     * @param songComment
     */
    public void setSongComment(String songComment) {
        this.songComment = songComment;
    }

    

    /**
     *
     * @return
     */
    public String getSongName() {
        return songName;
    }

    /**
     *
     * @param songName
     */
    public void setSongName(String songName) {
        this.songName = songName;
    }

    

    /**
     *
     * @return
     */
    public int getFuturDataSize() {
        return futurDataSize;
    }

    /**
     *
     * @param futurDataSize
     */
    public void setFuturDataSize(int futurDataSize) {
        this.futurDataSize = futurDataSize;
    }

    /**
     *
     * @return
     */
    public int getLoopFrames() {
        return loopFrames;
    }

    /**
     *
     * @param loopFrames
     */
    public void setLoopFrames(int loopFrames) {
        this.loopFrames = loopFrames;
    }

    /**
     *
     * @return
     */
    public int getClock() {
        return clock;
    }

    /**
     *
     * @param clock
     */
    public void setClock(int clock) {
        this.clock = clock;
    }

    /**
     *
     * @return
     */
    public short getDigidrums() {
        return digidrums;
    }

    /**
     *
     * @param digidrums
     */
    public void setDigidrums(short digidrums) {
        this.digidrums = digidrums;
    }

    /**
     *
     * @return
     */
    public short getFrequency() {
        return frequency;
    }

    /**
     *
     * @param frequency
     */
    public void setFrequency(short frequency) {
        this.frequency = frequency;
    }

    /**
     *
     * @return
     */
    public int getFrames() {
        return frames;
    }

    /**
     *
     * @param frames
     */
    public void setFrames(int frames) {
        this.frames = frames;
    }

    /**
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     * @return
     */
    public String getLeo() {
        return leo;
    }

    /**
     *
     * @param leo
     */
    public void setLeo(String leo) {
        this.leo = leo;
    }



    
}
