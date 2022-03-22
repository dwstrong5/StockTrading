/* Donald W. Strong
 *
 *  This class is used to define an Entry object that will be used in this assignment. This class outlines
 *  various constructors for an Entry object. An Entry object consists of eight fields:
 *      ticker = a string abbreviation for the company name (i.e. AAPL for apple, GOOG for google, etc.)
 *      date = a string representation of the date for the entry
 *      openingPrice = c double representing the opening price of the stock for that day
 *      highPrice = c double representing the highest price for the stock for that day
 *      lowPrice = c double representing the lowest price for that stock for that day
 *      closePrice = c double representing the closing price for that stock at the end of the day
 *      shares = an integer representing the volume or number of shares traded on that day
 *      adjClosingPrice = c double representing the adjusted price that includes corrections to the price for that day
 */
import java.lang.*;

public class Entry {

    /* Variable declarations */
    public String ticker;
    public String date;
    public double openingPrice;
    public double highPrice;
    public double lowPrice;
    public double closePrice;

    /* Constructor for a null Entry object */
    public Entry() {
    }

    /* Constructor to essentially "copy" an existing Entry object to a new Entry object */
    public Entry(Entry e) {
        this.ticker = e.ticker;
        this.date = e.date;
        this.openingPrice = e.openingPrice;
        this.highPrice = e.highPrice;
        this.lowPrice = e.lowPrice;
        this.closePrice = e.closePrice;
    }

    /* Constructor an Entry object in a given instance of time. This version will be used throughout the method that
       reads data from an input textfile
     */
    public Entry(String ticker, String date, double openingPrice, double highPrice, double lowPrice,
                 double closePrice) {

        this.ticker = ticker;
        this.date = date;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;


    }

    /* method used for testing purposes. Returns a String composed of all fields of an Entry object */
    public String entryToString() {
        String result = ticker + "\t" + date + "\t" + openingPrice + "\t" + highPrice + "\t" + lowPrice + "\t" +
                closePrice;
        return result;
    }

}

