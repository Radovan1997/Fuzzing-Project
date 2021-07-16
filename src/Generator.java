import java.io.FileWriter;
import java.io.FileReader;
import java.util.Random;
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.io.File;
import java.lang.*;

/** Generates completely random input files.
    Does not have to strictly adhere to CNF format
 */
public class Generator {

    public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String lower = upper.toLowerCase();
    public static final String digits = "0123456789\n";
    public static final String alphanum = upper + lower + digits;
    public static int lengthOfFile = 150300;



    public static void main(String[] args){

        //execute the dumb fuzzer
        dumbFuzzer(args[0]);
    }

    //Generates a random alphanumeric string, and returns this
    public static String generateRandomAlphanum(){
        Random random = new Random();
        char randChar = alphanum.charAt(random.nextInt(64));
        String retString = String.valueOf(randChar);

        return retString;
    }

    //For each line, generate a random alphanumeric string and append this to the string builder
    //Save the string builder output to a file, to be used as input for the SAT solvers
    public static void dumbFuzzer(String saveFileName){

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i<lengthOfFile; i++){
            sb.append(generateRandomAlphanum());
        }

        String output = sb.toString();

        try {

            Random random = new Random();
            //Generate a random file number
            int fileNum = random.nextInt(50000);
            File cnfFile = new File(saveFileName);
            cnfFile.createNewFile();
            FileWriter outWriter = new FileWriter(saveFileName);
            outWriter.write(output);
            outWriter.close();
        }

        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
