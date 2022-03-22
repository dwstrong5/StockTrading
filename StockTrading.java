
/* Donald W. Strong
 *
 * This program is intended to be used with the Entry class. Program allows the user to query a database (DB) and return
 * stock related information about a number of companies. The user is prompted by the console to enter a 'ticker' (or
 * abbreviation) for the desired company and optional start and end dates. If the company is found in the DB, the
 * program will print any stock splits detected throughout the data regarding the company, as well as perform an
 * "investment strategy" analysis and print the number of shares bought/sold and total net cash accrued to the User.
 */
import java.util.*;
import java.io.*;
import java.sql.*;
import java.lang.*;
import java.text.DecimalFormat;

public class StockTrading {

    static Connection conn = null;

    public static void main(String[] args) throws Exception {
        int isCompanyPresent;
        ArrayList<Entry> entryList;

        // Get connection properties
        String paramsFile = "readerparams.txt";
        if (args.length >= 1) {
            paramsFile = args[0];
        }
        Properties connectprops = new Properties();
        connectprops.load(new FileInputStream(paramsFile));

        try {
            // Get connection
            Class.forName("com.mysql.jdbc.Driver");
            String dburl = connectprops.getProperty("dburl");
            String username = connectprops.getProperty("user");
            conn = DriverManager.getConnection(dburl, connectprops);
            System.out.printf("Database connection %s %s established.%n", dburl, username);

            while (true) {

                /* Initial prompt for ticker and optional date */
                Scanner input = new Scanner(System.in);
                System.out.print("Enter ticker and date (YYYY.MM.DD): ");
                String[] userInput = input.nextLine().trim().split("\\s");

                /* If user types nothing or just whitespace, break the loop and terminate program */
                if (userInput[0].trim().length() == 0) {
                    break;
                }
                /* If user gives incorrect number of arguments */
                else if (userInput.length == 2 || userInput.length > 3) {
                    System.out.println("Incorrect usage. Enter ticker and optional start and end dates in the format" +
                            " YYYY.MM.DD");
                }
                /* User gave a valid query */
                else {
                    isCompanyPresent = getCompanyName(userInput[0]);

                    /* If we could not find the ticker in the DB */
                    if (isCompanyPresent == -1) {
                        System.out.println(userInput[0] + " not found in database.\n");
                    }
                    else {
                        /* If user specifies dates, call the version of getPriceVolume that takes in a ticker, start and
                           end dates as parameters
                         */
                        if (userInput.length == 3) {
                            entryList = getPriceVolume(userInput[0], userInput[1], userInput[2]);
                        }
                        /* If user does not specify dates, call the default version of getPriceVolume that only needs a
                           ticker
                         */
                        else {
                            entryList = getPriceVolume(userInput[0]);
                        }

                        /* Call method to perform an investment strategy analysis on the list returned by getPriceVolume
                         */
                        getTradingInfo(entryList);
                    }
                }

            }
            /* Close DB connection and terminate the program once the User is finished */
            conn.close();
            System.out.println("Database connection closed");
        }
        catch (SQLException ex) {
            System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
                    ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
        }
    }

    /* Method that takes in a ticker from the user and prints the name of the company to the console. This method
       returns an int that describes whether or not the query returns any results. If the company name is found in the
       DB, returns 0. Otherwise, returns -1 and prints an error to the user.
     */
    static int getCompanyName(String ticker) throws SQLException {
        Statement stmnt = conn.createStatement();
        ResultSet results = stmnt.executeQuery("select distinct Name from company " +
                "where Ticker = '" + ticker + "'");
        /* Returns a negative one if we don't retrieve any results */
        if (!results.isBeforeFirst()) {
            stmnt.close();
            return -1;
        }
        /* Prints the name of the company associated with ticker to the console and returns 0 */
        else {
            while (results.next()) {
                System.out.println(results.getString("Name"));
            }
            stmnt.close();
            return 0;
        }
    }

    /* Method that takes in a ticker specified by the user and calculates/identifies any stock splits that occur for
       all entries for that company in the DB. Method returns an ArrayList of Entry objects that consists of the results
       returned from the query. While generating the list, this method will detect either a 2:1, 3:1, or 3:2 stock split
       and adjust all subsequent price data with the appropriate divisor.
    */
    static ArrayList<Entry> getPriceVolume(String ticker) throws SQLException {
        ArrayList<Entry> list = new ArrayList<Entry>();
        double divisor = 1;
        Statement stmnt = conn.createStatement();
        double diff;
        Entry prev;
        int last = 0; //initial value for size of list. Workaround to avoid using size()-1 each iteration
        int numberOfStockSplits = 0;

        ResultSet results = stmnt.executeQuery("select TransDate, OpenPrice, HighPrice, LowPrice, ClosePrice" +
                " from pricevolume where Ticker = '" + ticker + "' order by TransDate DESC");

        /* Another check to ensure the ticker is found in the DB */
        if (!results.isBeforeFirst()) {
            System.out.println(ticker + " not found in database\n");
        }

        /* If we have results from the query... */
        else {
            /* Create Entry object and add first entry to ArrayList */
            while (results.next()) {

                /* If list has at least one element, grab most recent entry to compare for stockSplit calculations */
                if (!list.isEmpty()) {
                    prev = list.get(last);
                    diff = (Double.parseDouble(results.getString(5).trim()) / divisor) / prev.openingPrice;


                    /* If we detect a stock split, update the divisor accordingly */
                    /* If we detect a 2 to 1 stock split */
                    if (Math.abs(diff - 2.0) < 0.20) {
                        System.out.println("2:1 split on " + results.getString(1) + " " +
                                Double.parseDouble(results.getString(5).trim()) + " --> " + (prev.openingPrice * divisor));
                        divisor *= 2;
                        numberOfStockSplits++;

                    }
                    /* If we detect a 3 to 1 stock split */
                    else if (Math.abs(diff - 3.0) < 0.30) {
                        System.out.println("3:1 split on " + results.getString(1) + " " +
                                Double.parseDouble(results.getString(5).trim()) + " --> " + (prev.openingPrice * divisor));
                        divisor *= 3;
                        numberOfStockSplits++;

                    }
                    /* If we detect a 3 to 2 stock split */
                    else if (Math.abs(diff - 1.5) < 0.15) {
                        System.out.println("3:2 split on " + results.getString(1) + " " +
                                Double.parseDouble(results.getString(5).trim()) + " --> " + (prev.openingPrice * divisor));
                        divisor *= 1.5;
                        numberOfStockSplits++;
                    }

                    /* Create entry object with most recent divisor */
                    Entry currEntry = new Entry(ticker, results.getString(1),
                            Double.parseDouble(results.getString(2).trim()) / divisor,
                            Double.parseDouble(results.getString(3).trim()) / divisor,
                            Double.parseDouble(results.getString(4).trim()) / divisor,
                            Double.parseDouble(results.getString(5).trim()) / divisor);

                    list.add(currEntry);
                    last++;
                }
                /* It's the first entry... */
                else {
                    Entry currEntry = new Entry(ticker, results.getString(1),
                            Double.parseDouble(results.getString(2).trim()) / divisor,
                            Double.parseDouble(results.getString(3).trim()) / divisor,
                            Double.parseDouble(results.getString(4).trim()) / divisor,
                            Double.parseDouble(results.getString(5).trim()) / divisor);
                    list.add(currEntry);
                }
            }
            System.out.println(numberOfStockSplits + " splits in " + (++last) + " trading days\n");
        }
        return list;
    }

    /* Method that takes in a ticker and start and end dates specified by the user to be used in the query. Method
       calculates/identifies any stock splits that occur for all entries for that company in the DB. Method returns an
       ArrayList of Entry objects that consists of the results returned from the query. While generating the list,
       this method will detect either a 2:1, 3:1, or 3:2 stock split and adjust all subsequent price data with the
       appropriate divisor.
     */
    static ArrayList<Entry> getPriceVolume(String ticker, String beginningDate, String endDate) throws SQLException {
        ArrayList<Entry> list = new ArrayList<Entry>();
        double divisor = 1;
        Statement stmnt = conn.createStatement();
        double diff;
        Entry prev;
        int last = 0; //initial value for size of list. Workaround to avoid using size()-1 each iteration
        int numberOfStockSplits = 0;


        ResultSet results = stmnt.executeQuery("select TransDate, OpenPrice, HighPrice, LowPrice, ClosePrice" +
                " from pricevolume where Ticker = '" + ticker + "' and TransDate >= '" + beginningDate +
                "'  and TransDate <= '" + endDate + "' order by TransDate DESC");

        /* If we didn't return any results from the query */
        if (!results.isBeforeFirst()) {
            System.out.println(ticker + " not found in database\n");
        }
        /* If we have results from the query */
        else {
            /* Create Entry object and add first entry to ArrayList */
            while (results.next()) {

                /* If list has at least one element, grab most recent entry to compare for stockSplit calculations */
                if (!list.isEmpty()) {
                    prev = list.get(last);
                    diff = (Double.parseDouble(results.getString(5).trim()) / divisor) / prev.openingPrice;


                    /* If we detect a stock split, update the divisor accordingly */
                    /* If we detect a 2:1 stock split */
                    if (Math.abs(diff - 2.0) < 0.20) {
                        System.out.println("2:1 split on " + results.getString(1) + " " +
                                Double.parseDouble(results.getString(5).trim()) + " --> " + (prev.openingPrice * divisor));
                        divisor *= 2;
                        numberOfStockSplits++;

                    }
                    /* If we detect a 3:1 stock split */
                    else if (Math.abs(diff - 3.0) < 0.30) {
                        System.out.println("3:1 split on " + results.getString(1) + " " +
                                Double.parseDouble(results.getString(5).trim()) + " --> " + (prev.openingPrice * divisor));
                        divisor *= 3;
                        numberOfStockSplits++;

                    }
                    /* If we detect a 3:2 stock split */
                    else if (Math.abs(diff - 1.5) < 0.15) {
                        System.out.println("3:2 split on " + results.getString(1) + " " +
                                Double.parseDouble(results.getString(5).trim()) + " --> " + (prev.openingPrice * divisor));
                        divisor *= 1.5;
                        numberOfStockSplits++;
                    }

                    /* Create entry object with most recent divisor */
                    Entry currEntry = new Entry(ticker, results.getString(1),
                            Double.parseDouble(results.getString(2).trim()) / divisor,
                            Double.parseDouble(results.getString(3).trim()) / divisor,
                            Double.parseDouble(results.getString(4).trim()) / divisor,
                            Double.parseDouble(results.getString(5).trim()) / divisor);

                    list.add(currEntry);
                    last++;
                }
                /* It's the first entry... */
                else {
                    Entry currEntry = new Entry(ticker, results.getString(1),
                            Double.parseDouble(results.getString(2).trim()) / divisor,
                            Double.parseDouble(results.getString(3).trim()) / divisor,
                            Double.parseDouble(results.getString(4).trim()) / divisor,
                            Double.parseDouble(results.getString(5).trim()) / divisor);
                    list.add(currEntry);
                }
            }
            System.out.println(numberOfStockSplits + " splits in " + (++last) + " trading days\n");
        }
        return list;
    }

    /* Method that takes in an ArrayList of Entry objects (output from getPriceVolume) and performs an "investment
       strategy" analysis on the list. This method iterates through the list and establishes/maintains a rolling average
       of closing prices for the previous 50 trading days. If certain critera are met for the current day, the method
       will either purchase or sell stock shares for that day. Once finished iterating through the list, the method will
       further liquidate any remaining shares. Finally, method prints the total number of transactions as well as net
       cash accrued to the user
     */
    public static void getTradingInfo(ArrayList<Entry> list) {
        Collections.reverse(list);
        int size = list.size();
        int curr = 50;
        Entry currEntry;
        int fiftyDaysPrior = 0;
        double avgClosePrice;
        double cash = 0;
        int transCount = 0;
        int numOfShares = 0;
        DecimalFormat df = new DecimalFormat("#.##");

        /* If the query gave us more than 51 results, we can generate a rolling average for closing prices */
        if (size > 51) {

            /* Loop through until we reach the last trading day */
            while (curr < size - 1) {

                /* Today's entry */
                currEntry = list.get(curr);

                /* Find the rolling average from past 50 days */
                avgClosePrice = 0;
                for (int i = fiftyDaysPrior; i < curr; i++) {
                    avgClosePrice += list.get(i).closePrice;
                }
                avgClosePrice /= 50;

                /* Buying criteria - if today's closing price is less than the average closing price AND  today's closing
                *  price is less than 3% of today's opening price.*/
                if (currEntry.closePrice < avgClosePrice && (currEntry.closePrice/currEntry.openingPrice < 0.97000001))
                {
                    transCount++;
                    numOfShares += 100;
                    cash -= (100 * list.get(curr+1).openingPrice);
                    cash -= 8;
                }
                /* Selling criteria - if we have shares to sell AND today's opening price is greater than the average
                   closing price AND today's opening price exceeds yesterdays closing price by 1% or more.
                 */
                else if (numOfShares >= 100 && currEntry.openingPrice > avgClosePrice && (currEntry.openingPrice /
                        list.get(curr-1).closePrice) > 1.00999999) {
                    double todaysAvgPrice = (currEntry.openingPrice + currEntry.closePrice) / 2;
                    transCount++;
                    numOfShares -= 100;
                    cash += (100 * todaysAvgPrice);
                    cash -= 8;
                }

                curr++;
                fiftyDaysPrior++;
            }

            /* If we have any remaining shares on the last trading day, liquidate */
            if (numOfShares > 0) {
                cash += (list.get(curr).openingPrice * numOfShares);
                transCount++;
            }

            /* Print out the results after all trading is finished for the company */
            System.out.println("Executing investment strategy");
            System.out.println("Transactions executed: " + transCount);
            System.out.println("Net cash: " + df.format(cash) + "\n");

        }

    }
}


