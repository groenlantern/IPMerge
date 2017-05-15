/*
 * Read in Files and combine contents as output 
 * 
 * Your program must accept two filenames as command-line parameters.
 * These files will contain 7-bit ASCII text, and each line may consist of an IP address, followed by a
 * colon, followed by a comma-separated list of numbers.
 * The two files should be joined on IP address and the numbers from each file should be appended and
 * returned, sorted and without duplicates.
 * The results should be written to stdout as the IP address followed by a colon, followed by a comma
 * separated list of the numbers.
 * 
 * Your program should handle errors, including malformed input,
 * appropriately and should be of a sufficient quality that it can run on a production Linux system
 */
package javaip_reader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author Jean-Pierre Erasmus
 */
public class JavaIP_Reader {

    //Constant REGEX to match IP Address, number list
    private static final String IP_REGEX = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static final String NO_REGEX = "^(\\d+(,\\d+)*)?$";
    private static final String LOGFILEOUT = "/var/logs/JavaIP_Reader.log";
    private static final String FILEOUT = "/tmp/JavaIP_Reader.out";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //test for valid filename inputs and files 
            if (args.length < 2) {
                //Error - need at least 2 file names
                writeLog("#110011 - Need at least 2 file names");
                return;
            }

            if (args.length > 2) writeLog("#110022 - More than two parameters passed, ignoring the rest");
            
            //Get input filenames
            String filename1 = args[0];
            String filename2 = args[1];

            //Check filenames
            if (filename1 != null && filename1.trim().isEmpty()) {
                //error 
                writeLog("#130011 - Filename 1 invalid - " + filename1);
                return;
            }

            if (filename2 != null && filename2.trim().isEmpty()) {
                //error 
                writeLog("#130022 - Filename 2 invalid - " + filename1);
                return;
            }

            //Output heading
            writeLog("#00000 - IP Number list merge : " + filename1 + " and " + filename2);
            writeOut("#00000 - IP Number list merge : " + filename1 + " and " + filename2);

            //Create list to store IP Date from files
            TreeMap<String, TreeMap<Integer, Integer>> ipList = new TreeMap<>();

            //Process File data file 1 
            ipList = getMapFromStream(ipList, filename1);
            //Process File data file 2 
            ipList = getMapFromStream(ipList, filename2);

            //Print Sorted, merged list by ip numbers 
            //Copy the ip list to be used in lambda expression
            printSortedList ( ipList ) ;
            
        } catch (Exception e) {
            //Handle any unexpected exceptions
            writeLog("#20002 - Main Processing Exception - " + e.getMessage());
        }
        //Log Completed
    }

    /**
     * Print the sorted merged list
     * @param ipListCopy 
     */
    final static void printSortedList ( final TreeMap<String, TreeMap<Integer, Integer>> ipListCopy ) { 

        try {     
            //Get all Entries and print out list 
            ipListCopy.keySet().forEach((ipNumber) -> {
                TreeMap<Integer, Integer> numberList = ipListCopy.get(ipNumber);
                
                String outText = ipNumber + ": ";

                int x = 0;
                //Print number list, comma seperated
                for (Integer sortedNumber : numberList.values()) {
                    if (x++ > 0) {
                        outText += ",";
                    }
                    outText += sortedNumber;
                }
                //Print message to screen and output file in /tmp
                writeOut (outText);
            });
        } catch (Exception eP) {
            //Handle any unexpected exceptions
            writeLog("#450023 - Printing Processing Exception - " + eP.getMessage());
        }
    }

    /**
     * Extract and validate data in files Create Sorted TreeMap with merged
     * numbers list by IP number
     *
     * @param ipList
     * @param filename
     * @return
     */
    public static TreeMap<String, TreeMap<Integer, Integer>> getMapFromStream(TreeMap<String, TreeMap<Integer, Integer>> ipList, String filename) {

        //Process File data from the file
        try (Stream<String> fileStream = Files.lines(Paths.get(filename));) {

            //Process file, read each line
            fileStream.forEach(lineData -> {
                //Validate line data, remove spaces
                String[] ipSplit = lineData.replaceAll(" ", "").split(":");

                if (ipSplit == null || lineData.trim().isEmpty()
                        || ipSplit.length < 2 || ipSplit.length > 2) {
                    //Error - Invalid Line data, skip line 
                    writeLog("#140011 - Ip/List Validation Error - " + lineData);

                    return;
                }

                //Validate ipnumber 
                String ipNo = ipSplit[0];

                if (!ipAddressValidator(ipNo)) {
                    //Error Invalid ip number 
                    //Skip line 
                    writeLog("#140022 - Ip Address Validation Error - " + ipNo);
                    return;
                }

                //Validate list 
                String noList = ipSplit[1];
                if (!noListValidator(noList)) {
                    //Error invalid number list 
                    //Skip Line 
                    writeLog("#140033 - Number List Validation Error - " + noList);
                    return;
                }
                //Create array from number list 
                List<String> numArray = Arrays.asList(noList.split(","));

                //Create new Entry into treemap 
                //Check if key exist, if key exist, merge current numberslist to the list in the existing entry. 
                if (ipList.containsKey(ipNo)) {

                    numArray.forEach((cellNumber) -> {
                        Integer numberCast = Integer.parseInt(cellNumber);

                        ipList.get(ipNo).put(numberCast, numberCast);
                    });
                } else {
                    //Create new treemap of numbers
                    TreeMap<Integer, Integer> tempNoList = new TreeMap<>();

                    //Add numbers to tree
                    numArray.forEach((celNo) -> {
                        Integer numberCast = Integer.parseInt(celNo);

                        tempNoList.put(numberCast, numberCast);
                    });
                    //Add new entry to iplist
                    ipList.put(ipNo, tempNoList);
                }
            });

        } catch (Exception e) {
            //Error - write to log file
            writeLog("#150011 - File Stream Data Exception - " + e.getMessage());
        }

        //Return modified iplist 
        return ipList;
    }

    /**
     * Validate IP Number based on REGEX String
     *
     * @param ipNo
     * @return
     */
    public static boolean ipAddressValidator(String ipNo) {
        return Pattern.compile(IP_REGEX).matcher(ipNo).matches();
    }

    /**
     * Validate number list based on REGEX
     *
     * @param noList
     * @return
     */
    public static boolean noListValidator(String noList) {
        return Pattern.compile(NO_REGEX).matcher(noList).matches();
    }

    /**
     * Write Error Log
     *
     * @param message
     */
    public static void writeLog(String message) {
        writeFile(LOGFILEOUT, message);        
    }

    /**
     * Write Output Log
     *
     * @param message
     */
    public static void writeOut(String message) {
        writeFile(FILEOUT, message);        
        System.out.println(message);
    }
    
    /**
     * Write Out File 
     *
     * @param FileName
     * @param message
     */
    public static void writeFile(String FileName, String message) {
        //Open file, make sure file exist if not create 
        File outFile = new File(FileName);

        //Create file if not exist
        if (!outFile.exists()) {
            try {
                outFile.createNewFile();
            } catch (IOException ex) {
                System.out.println("Exception Creating File Error #30003: " + ex.getMessage());
            }
        }

        try (FileWriter fileWriter = new FileWriter(outFile.getAbsoluteFile(), true);
                BufferedWriter buffer = new BufferedWriter(fileWriter);) {

            //Write message to File             
            buffer.write((new Date()) + " - " + message + "\r\n");
        } catch (Exception elog) {
            System.out.println("File Write Error #10001: " + elog.getMessage());
        }
    }

}
