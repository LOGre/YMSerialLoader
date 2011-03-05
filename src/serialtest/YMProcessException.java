/*
 * YMProcessException Class
 */

package serialtest;

/**
 *
 * @author shazz
 */
class YMProcessException extends Exception
{

    public YMProcessException(Throwable thrwbl) 
    {
        super( thrwbl );
    }

    public YMProcessException(String string) 
    {
        super (string);
    }

    public YMProcessException(String string, Throwable thrwbl)
    {
        super( string, thrwbl );
    }



    
}
